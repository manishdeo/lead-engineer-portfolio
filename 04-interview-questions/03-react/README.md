# ⚛️ React JS Interview Questions (180+)

---

### Q1: What are React Server Components? How do they differ from Client Components?
**A:** Server Components render on the server, send HTML (zero JS to client). Client Components are interactive, hydrated on client. Use Server Components for data fetching and static content. Use Client Components for interactivity (onClick, useState).

### Q2: Explain React's reconciliation algorithm (Fiber).
**A:** React Fiber enables incremental rendering — breaks work into chunks, can pause/resume. Uses a virtual DOM diff algorithm: same type = update, different type = unmount/remount. Keys help identify moved elements in lists.

### Q3: How do you optimize React performance?
**A:**
- `React.memo` for pure components
- `useMemo` / `useCallback` for expensive computations
- Code splitting with `React.lazy` + `Suspense`
- Virtualization for long lists (`react-window`)
- Avoid inline objects/functions in JSX
- Use React DevTools Profiler to identify bottlenecks

### Q4: What is the difference between useEffect, useLayoutEffect, and useInsertionEffect?
**A:**
- `useEffect` — Runs after paint (async). Most common.
- `useLayoutEffect` — Runs before paint (sync). For DOM measurements.
- `useInsertionEffect` — Runs before DOM mutations. For CSS-in-JS libraries.

### Q5: How do you manage state in a large React application?
**A:**
- **Local:** `useState`, `useReducer`
- **Server state:** TanStack Query (caching, refetching, optimistic updates)
- **Global client:** Zustand (simple) or Redux Toolkit (complex)
- **URL state:** Search params via `useSearchParams`
- **Form state:** React Hook Form

### Q6: What are custom hooks? When should you create one?
**A:** Extract reusable stateful logic. Create when: same logic used in 2+ components, complex state management, side effects that need cleanup. Example: `useDebounce`, `useLocalStorage`, `useAuth`.

### Q7: Explain React's Context API limitations.
**A:** Every consumer re-renders when context value changes (even if they only use part of it). Solutions: split contexts, memoize values, use state management library for frequent updates.
