# 🎨 Frontend Architecture — Interview Reference

---

## Micro-Frontend Architecture

```
┌─────────────────────────────────────────────┐
│              App Shell (Host)                │
│  ┌──────────┐ ┌──────────┐ ┌──────────┐   │
│  │  Header   │ │  Product │ │  Cart    │   │
│  │  (Team A) │ │  (Team B)│ │ (Team C) │   │
│  │  Remote   │ │  Remote  │ │  Remote  │   │
│  └──────────┘ └──────────┘ └──────────┘   │
│                                             │
│  Shared: Design System, Auth, Analytics     │
└─────────────────────────────────────────────┘
```

**Module Federation (Webpack 5):** Load remote modules at runtime. Each team deploys independently.

## SSR vs CSR vs SSG vs ISR

| Strategy | Rendering | Best For |
|----------|-----------|----------|
| **CSR** | Client-side | SPAs, dashboards |
| **SSR** | Server per request | SEO, dynamic content |
| **SSG** | Build time | Blogs, docs, marketing |
| **ISR** | Revalidate on interval | E-commerce catalogs |

## React Performance Patterns
- `React.memo` — Prevent unnecessary re-renders
- `useMemo` / `useCallback` — Memoize expensive computations/callbacks
- Code splitting — `React.lazy` + `Suspense`
- Virtualization — `react-window` for large lists
- Server Components (React 19) — Zero client JS for static content

## State Management Decision
- **Local state** → `useState` / `useReducer`
- **Server state** → TanStack Query (React Query)
- **Global client state** → Zustand (simple) or Redux Toolkit (complex)
- **URL state** → Search params
