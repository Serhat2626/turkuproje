# Anadolu Atlası — Admin Panel

AI destekli çok kanallı içerik üretim platformu yönetim paneli.

## Tech Stack

- **React** 19 + TypeScript
- **Vite** 6 (build & dev server)
- **react-router-dom** 7 (client-side routing)
- **Vanilla CSS** (premium dark theme, glassmorphism)

## Geliştirme

```bash
# Bağımlılıkları yükle
npm install

# Dev server başlat (port 5173)
npm run dev

# Production build
npm run build

# Build önizleme
npm run preview
```

## API Proxy

Dev modunda `/api/*` istekleri otomatik olarak `http://localhost:8080` adresine proxy edilir. Backend'in çalışıyor olduğundan emin olun.

## Yapı

```
src/
├── main.tsx               # Entry point
├── App.tsx                # Route tanımları
├── index.css              # Global stiller
├── vite-env.d.ts          # Vite tip tanımları
├── types/
│   └── index.ts           # Paylaşılan tipler
├── components/
│   └── layout/
│       ├── Layout.tsx     # Ana layout (sidebar + content)
│       └── Sidebar.tsx    # Navigasyon sidebar
└── pages/
    └── DashboardPage.tsx  # Ana dashboard sayfası
```

## Docker

```bash
docker build -t admin-web .
docker run -p 5173:5173 admin-web
```
