# ================================================================
# Anadolu Atlası Admin Web - Multi-stage Docker Build
# ================================================================
# React 19 + Vite + TypeScript → Nginx
# ================================================================

# ── Stage 1: Build ──────────────────────────────────────
FROM node:22-alpine AS builder
WORKDIR /app

# Bağımlılık dosyalarını önce kopyala (layer cache)
COPY package.json package-lock.json* ./
RUN npm ci --no-audit --no-fund

# Kaynak kodu kopyala ve build et
COPY . .
RUN npm run build

# ── Stage 2: Serve ──────────────────────────────────────
FROM nginx:alpine

# Nginx konfigürasyonu
COPY nginx.conf /etc/nginx/conf.d/default.conf

# Build çıktısını kopyala
COPY --from=builder /app/dist /usr/share/nginx/html

EXPOSE 5173

CMD ["nginx", "-g", "daemon off;"]
