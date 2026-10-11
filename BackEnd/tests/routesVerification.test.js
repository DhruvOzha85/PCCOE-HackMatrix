const { test, describe, before, after } = require('node:test');
const assert = require('node:assert');
const http = require('node:http');

const app = require('../src/app');

describe('22 Backend Routes Mounted & Verification Tests', () => {
    let server;
    let baseUrl;

    before((_, done) => {
        server = http.createServer(app);
        server.listen(0, '127.0.0.1', () => {
            const port = server.address().port;
            baseUrl = `http://127.0.0.1:${port}`;
            done();
        });
    });

    after((_, done) => {
        server.close(done);
    });

    const routesToCheck = [
        { method: 'GET', path: '/', expectedStatus: [200] },
        { method: 'GET', path: '/health/cron', expectedStatus: [200] },
        { method: 'POST', path: '/api/users/register', expectedStatus: [400, 422] },
        { method: 'POST', path: '/api/users/login', expectedStatus: [400, 422] },
        { method: 'POST', path: '/api/users/refresh', expectedStatus: [400, 401] },
        { method: 'GET', path: '/api/users/me', expectedStatus: [401] },
        { method: 'POST', path: '/api/users/logout', expectedStatus: [200, 401] },
        { method: 'GET', path: '/api/users/profile', expectedStatus: [401] },
        { method: 'PUT', path: '/api/users/profile', expectedStatus: [401] },
        { method: 'GET', path: '/api/dashboard', expectedStatus: [401] },
        { method: 'POST', path: '/api/documents/process', expectedStatus: [401] },
        { method: 'POST', path: '/api/documents/extract', expectedStatus: [401] },
        { method: 'GET', path: '/api/documents', expectedStatus: [401] },
        { method: 'GET', path: '/api/documents/00000000-0000-0000-0000-000000000001', expectedStatus: [401] },
        { method: 'DELETE', path: '/api/documents/00000000-0000-0000-0000-000000000001', expectedStatus: [401] },
        { method: 'POST', path: '/api/schemes/search', expectedStatus: [200] },
        { method: 'GET', path: '/api/schemes/test-scheme-id', expectedStatus: [404, 500, 200] },
        { method: 'POST', path: '/api/eligibility/check', expectedStatus: [401] },
        { method: 'POST', path: '/api/chat', expectedStatus: [200, 400] },
        { method: 'GET', path: '/api/applications', expectedStatus: [401] },
        { method: 'POST', path: '/api/applications', expectedStatus: [401] },
        { method: 'GET', path: '/api/applications/00000000-0000-0000-0000-000000000001', expectedStatus: [401] },
        { method: 'POST', path: '/api/applications/analyze', expectedStatus: [401] }
    ];

    test('All 23 Backend routes are mounted and not returning 404 (Route not found)', async () => {
        assert.strictEqual(routesToCheck.length, 23, 'Must test exactly 23 documented routes');

        for (const route of routesToCheck) {
            const res = await fetch(`${baseUrl}${route.path}`, {
                method: route.method,
                headers: { 'Content-Type': 'application/json' },
                body: ['POST', 'PUT'].includes(route.method) ? JSON.stringify({}) : undefined
            });

            // If a route returns 404 with "Route not found", then the router failed to mount it
            if (res.status === 404) {
                const text = await res.text();
                assert.ok(
                    !text.includes('Route not found'),
                    `Route ${route.method} ${route.path} is not mounted! Got: ${text}`
                );
            }

            assert.ok(
                route.expectedStatus.includes(res.status),
                `Route ${route.method} ${route.path} returned unexpected status ${res.status}. Expected one of: ${route.expectedStatus.join(', ')}`
            );
        }
    });

    test('POST /api/users/login route is canonically mounted and handles auth validation', async () => {
        const res = await fetch(`${baseUrl}/api/users/login`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({})
        });

        const body = await res.json();
        assert.strictEqual(res.status, 400, 'Empty login body must return HTTP 400 Bad Request');
        assert.strictEqual(body.success, false);
        assert.ok(
            body.message.includes('Email and password are required'),
            `Expected validation message, got: ${body.message}`
        );
    });

    test('POST /api/api/users/login gracefully normalizes duplicate /api prefix and reaches login route', async () => {
        const res = await fetch(`${baseUrl}/api/api/users/login`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({})
        });

        const body = await res.json();
        assert.notStrictEqual(res.status, 404, 'Must not return 404 Route not found for /api/api/users/login');
        assert.strictEqual(res.status, 400, 'Normalized endpoint must execute loginUser validation and return 400');
        assert.strictEqual(body.success, false);
        assert.ok(body.message.includes('Email and password are required'));
    });

    test('GET /health/cron returns HTTP 200 with minimal { "status": "ok" } JSON', async () => {
        const res = await fetch(`${baseUrl}/health/cron`);
        assert.strictEqual(res.status, 200, 'Health cron endpoint must return HTTP 200');

        const contentType = res.headers.get('content-type') || '';
        assert.ok(contentType.includes('application/json'), `Expected application/json, got ${contentType}`);

        const text = await res.text();
        assert.ok(text.length < 100, `Health cron response must be minimal, got length: ${text.length}`);

        const body = JSON.parse(text);
        assert.deepStrictEqual(body, { status: 'ok' }, 'Response must be strictly { status: "ok" }');
        assert.strictEqual(Object.keys(body).length, 1, 'Response must contain only the status key');
    });
});
