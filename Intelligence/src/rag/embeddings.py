"""
FIN Embedding Abstraction Layer.
Defines a pluggable interface for dense embeddings supporting production SentenceTransformers
(e.g. BAAI/bge-m3) and deterministic mock embeddings for offline unit testing.
"""

from abc import ABC, abstractmethod
import hashlib
import logging
import os
from typing import List, Optional
import numpy as np

import sys
from pathlib import Path

_CUR = Path(__file__).resolve()
while _CUR.name != "Intelligence" and _CUR.parent != _CUR:
    _CUR = _CUR.parent
_INTELLIGENCE_DIR = _CUR
if str(_INTELLIGENCE_DIR) not in sys.path:
    sys.path.insert(0, str(_INTELLIGENCE_DIR))

try:
    from .config import RAGConfig, DEFAULT_RAG_CONFIG
except (ImportError, ValueError):
    from src.rag.config import RAGConfig, DEFAULT_RAG_CONFIG

logger = logging.getLogger("fin.rag.embeddings")


class EmbeddingModel(ABC):
    """Abstract Base Class for Dense Embedding Models."""

    @property
    @abstractmethod
    def dimension(self) -> int:
        """Returns the embedding vector dimensionality."""
        pass

    @property
    @abstractmethod
    def model_name(self) -> str:
        """Returns the identifier or path of the model."""
        pass

    @abstractmethod
    def encode_documents(self, texts: List[str], batch_size: int = 64) -> np.ndarray:
        """Encodes a list of document chunk texts into a 2D float32 numpy array."""
        pass

    @abstractmethod
    def encode_query(self, query: str) -> np.ndarray:
        """Encodes a single user query into a 1D float32 numpy array."""
        pass


class SentenceTransformerEmbeddingModel(EmbeddingModel):
    """
    Production embedding model leveraging sentence-transformers (e.g. BAAI/bge-m3).
    Supports multilingual English/Hindi queries with automatic graceful degradation
    to deterministic offline embeddings if model loading fails or weights are absent.
    """

    def __init__(
        self,
        model_name: str = "BAAI/bge-m3",
        dimension: int = 1024,
        normalize: bool = True
    ):
        self._model_name = model_name
        self._dimension = dimension
        self._normalize = normalize
        self._model = None
        self._fallback = None

    def _load_model(self):
        if self._model is not None or self._fallback is not None:
            return
        try:
            from sentence_transformers import SentenceTransformer
            logger.info("Loading sentence-transformers embedding model: %s", self._model_name)
            self._model = SentenceTransformer(self._model_name)
            sample_emb = self._model.encode("test", convert_to_numpy=True)
            self._dimension = int(sample_emb.shape[-1])
            logger.info("Embedding model %s loaded successfully (dimension=%d)", self._model_name, self._dimension)
        except Exception as e:
            logger.warning(
                "Could not load sentence-transformers model '%s' (%s). "
                "Degrading gracefully to DeterministicMockEmbeddingModel.",
                self._model_name, e
            )
            self._fallback = DeterministicMockEmbeddingModel(dimension=self._dimension)

    @property
    def dimension(self) -> int:
        return self._dimension

    @property
    def model_name(self) -> str:
        return self._model_name

    def encode_documents(self, texts: List[str], batch_size: int = 64) -> np.ndarray:
        self._load_model()
        if self._fallback is not None:
            return self._fallback.encode_documents(texts, batch_size=batch_size)
        if not texts:
            return np.empty((0, self._dimension), dtype=np.float32)

        embeddings = self._model.encode(
            texts,
            batch_size=batch_size,
            show_progress_bar=False,
            normalize_embeddings=self._normalize,
            convert_to_numpy=True
        )
        return embeddings.astype(np.float32)

    def encode_query(self, query: str) -> np.ndarray:
        self._load_model()
        if self._fallback is not None:
            return self._fallback.encode_query(query)
        if not query.strip():
            return np.zeros(self._dimension, dtype=np.float32)

        embedding = self._model.encode(
            query,
            normalize_embeddings=self._normalize,
            convert_to_numpy=True
        )
        return embedding.astype(np.float32)


class DeterministicMockEmbeddingModel(EmbeddingModel):
    """
    Deterministic, offline embedding model for unit testing and offline environments.
    Produces stable pseudo-dense vectors derived from token hashes.
    Guarantees that identical text yields identical vectors without model downloads.
    """

    def __init__(self, dimension: int = 128, model_name: str = "mock-deterministic-v1"):
        self._dimension = dimension
        self._model_name = model_name

    @property
    def dimension(self) -> int:
        return self._dimension

    @property
    def model_name(self) -> str:
        return self._model_name

    def _hash_text_to_vector(self, text: str) -> np.ndarray:
        if not text.strip():
            return np.zeros(self._dimension, dtype=np.float32)

        vec = np.zeros(self._dimension, dtype=np.float32)
        words = text.lower().split()
        for word in words:
            # Hash each word into index and sign
            h = int(hashlib.md5(word.encode("utf-8")).hexdigest(), 16)
            idx = h % self._dimension
            sign = 1.0 if ((h >> 4) % 2 == 0) else -1.0
            vec[idx] += sign

        norm = np.linalg.norm(vec)
        if norm > 0:
            vec = vec / norm
        return vec

    def encode_documents(self, texts: List[str], batch_size: int = 64) -> np.ndarray:
        if not texts:
            return np.empty((0, self._dimension), dtype=np.float32)
        vectors = [self._hash_text_to_vector(t) for t in texts]
        return np.vstack(vectors).astype(np.float32)

    def encode_query(self, query: str) -> np.ndarray:
        return self._hash_text_to_vector(query)


def get_embedding_model(
    config: RAGConfig = DEFAULT_RAG_CONFIG,
    force_mock: bool = False
) -> EmbeddingModel:
    """Factory creating configured embedding model."""
    use_mock = (
        force_mock
        or getattr(config, "use_mock_embeddings", False)
        or os.getenv("USE_MOCK_EMBEDDINGS", "false").strip().lower() in ("true", "1", "yes")
    )
    if use_mock:
        logger.info(
            "Initializing DeterministicMockEmbeddingModel (dimension=%d) per USE_MOCK_EMBEDDINGS configuration.",
            config.embedding_dimension
        )
        return DeterministicMockEmbeddingModel(dimension=config.embedding_dimension)
    try:
        return SentenceTransformerEmbeddingModel(
            model_name=config.embedding_model_name,
            dimension=config.embedding_dimension,
            normalize=config.normalize_embeddings
        )
    except Exception as e:
        logger.warning("Failed to initialize SentenceTransformerEmbeddingModel (%s); falling back to DeterministicMockEmbeddingModel", e)
        return DeterministicMockEmbeddingModel(dimension=config.embedding_dimension)