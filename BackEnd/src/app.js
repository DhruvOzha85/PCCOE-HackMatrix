const express = require("express");
const userRoutes = require("./routes/userRouter");
const profileRoutes = require("./routes/profileRouter");
const dashboardRoutes = require("./routes/dashboardRouter");
const documentRoutes = require("./routes/documentRoutes");
const schemeRoutes = require("./routes/schemeRoutes");
const eligibilityRoutes = require("./routes/eligibilityRoutes");
const chatRoutes = require("./routes/chatRoutes");
const applicationRoutes = require("./routes/applicationRoutes");
const reviewerRoutes = require("./routes/reviewerRoutes");
const notificationRoutes = require("./routes/notificationRoutes");
const translateRoutes = require("./routes/translateRoutes");
const { notFoundHandler, globalErrorHandler } = require("./middleware/errorHandler");

const { createCorsMiddleware } = require("./config/corsConfig");

const app = express();

app.use(createCorsMiddleware());
app.use(express.json());

// Normalize accidental double /api/api prefixes from misconfigured frontend clients
app.use((req, res, next) => {
    if (req.url && req.url.startsWith('/api/api/')) {
        req.url = req.url.replace(/^\/api\/api\//, '/api/');
    }
    next();
});

app.get("/", (req, res) => {
    res.json({
        success: true,
        message: "Backend API is running"
    });
});

app.get("/health/cron", (req, res) => {
    res.status(200).json({
        status: "ok"
    });
});

app.use("/api/users", userRoutes);
app.use("/api/users", profileRoutes);
app.use("/api/dashboard", dashboardRoutes);
app.use("/api/documents", documentRoutes);
app.use("/api/schemes", schemeRoutes);
app.use("/api/eligibility", eligibilityRoutes);
app.use("/api/chat", chatRoutes);
app.use("/api/applications", applicationRoutes);
app.use("/api/reviewer", reviewerRoutes);
app.use("/api/notifications", notificationRoutes);
app.use("/api/translate", translateRoutes);
app.use(notFoundHandler);
app.use(globalErrorHandler);

module.exports = app;
