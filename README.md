# CloudVault

Cloud-based meta file storage service built with Java 17 / Spring Boot and React / Vite.

## Production architecture

React/Vite (Vercel) -> Spring Boot API (Render) -> PostgreSQL (Supabase) + Supabase Storage.

## Local development

Backend:
```powershell
cd backend
mvn spring-boot:run
```

Frontend:
```powershell
cd frontend
npm install
npm run dev
```

Set `VITE_API_URL=http://localhost:8081/api` for local development.

## Production environment

Backend:
- PORT
- DB_URL
- DB_USERNAME
- DB_PASSWORD
- JWT_SECRET
- JWT_EXPIRATION_MS
- CORS_ALLOWED_ORIGINS
- STORAGE_TYPE=supabase
- SUPABASE_URL
- SUPABASE_STORAGE_BUCKET
- SUPABASE_SERVICE_ROLE_KEY
- Optional SMTP variables

Frontend:
- VITE_API_URL=https://YOUR-RENDER-SERVICE.onrender.com/api

Never commit `.env` files or service-role keys.
