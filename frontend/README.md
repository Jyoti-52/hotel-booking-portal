# Frontend (Angular)

```bash
npm install
npm start        # http://localhost:4200, proxies /api to http://localhost:8080
npm run build    # production build in dist/
```

`proxy.conf.json` forwards `/api` calls to the Spring Boot backend, so no CORS setup is needed in development.
