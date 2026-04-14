# ⚛️ React JS Advanced - Interview Q&A

> 180+ Questions with Answers, Examples & Latest Trends (2024-2026)

---

## 📋 Table of Contents

1. [React 18+ Features](#1-react-18-features)
2. [React 19 & Server Components](#2-react-19-server-components)
3. [Hooks Deep Dive](#3-hooks-deep-dive)
4. [Performance Optimization](#4-performance-optimization)
5. [State Management](#5-state-management)
6. [Testing](#6-testing)

---

## 1. React 18+ Features

### Q1: Explain Concurrent Rendering in React 18. How does it improve performance?

**Answer:**

**Concurrent Rendering:**
- Allows React to interrupt rendering
- Prioritizes urgent updates
- Keeps UI responsive
- Uses time-slicing

**Example:**

```jsx
import { useState, useTransition, useDeferredValue } from 'react';

// useTransition - Mark non-urgent updates
function SearchComponent() {
  const [query, setQuery] = useState('');
  const [results, setResults] = useState([]);
  const [isPending, startTransition] = useTransition();
  
  const handleSearch = (e) => {
    const value = e.target.value;
    setQuery(value); // Urgent update - immediate
    
    // Non-urgent update - can be interrupted
    startTransition(() => {
      const filtered = searchDatabase(value); // Expensive operation
      setResults(filtered);
    });
  };
  
  return (
    <div>
      <input 
        value={query} 
        onChange={handleSearch}
        placeholder="Search..."
      />
      {isPending && <Spinner />}
      <ResultsList results={results} />
    </div>
  );
}

// useDeferredValue - Defer expensive renders
function ProductList({ searchQuery }) {
  const deferredQuery = useDeferredValue(searchQuery);
  const products = useMemo(
    () => filterProducts(deferredQuery),
    [deferredQuery]
  );
  
  return (
    <div>
      {products.map(product => (
        <ProductCard key={product.id} product={product} />
      ))}
    </div>
  );
}
```

**Suspense for Data Fetching:**

```jsx
import { Suspense } from 'react';

// Resource-based data fetching
const resource = fetchUserData();

function UserProfile() {
  const user = resource.read(); // Suspends if not ready
  
  return (
    <div>
      <h1>{user.name}</h1>
      <p>{user.email}</p>
    </div>
  );
}

function App() {
  return (
    <Suspense fallback={<ProfileSkeleton />}>
      <UserProfile />
    </Suspense>
  );
}
```

**Interview Tip:** Emphasize that Concurrent Rendering makes apps feel faster by keeping UI responsive during heavy computations.

---

## 2. React 19 & Server Components

### Q2: What are React Server Components? How do they differ from Client Components?

**Answer:**

**Server Components (React 19):**
- Render on server only
- Zero JavaScript to client
- Direct database access
- Better performance
- Automatic code splitting

**Client Components:**
- Render on client
- Interactive (useState, useEffect)
- Event handlers
- Browser APIs

**Example:**

```jsx
// app/page.tsx (Server Component - default)
import { db } from '@/lib/database';

// This runs ONLY on server
async function ProductsPage() {
  // Direct database access - no API needed
  const products = await db.product.findMany({
    include: { reviews: true }
  });
  
  return (
    <div>
      <h1>Products</h1>
      {products.map(product => (
        <ProductCard key={product.id} product={product} />
      ))}
    </div>
  );
}

// components/ProductCard.tsx (Server Component)
async function ProductCard({ product }) {
  // Can fetch additional data
  const relatedProducts = await db.product.findMany({
    where: { category: product.category },
    take: 3
  });
  
  return (
    <div>
      <h2>{product.name}</h2>
      <p>{product.price}</p>
      <AddToCartButton productId={product.id} /> {/* Client Component */}
      <RelatedProducts products={relatedProducts} />
    </div>
  );
}

// components/AddToCartButton.tsx (Client Component)
'use client'; // Marks as Client Component

import { useState } from 'react';

export function AddToCartButton({ productId }) {
  const [isAdding, setIsAdding] = useState(false);
  
  const handleClick = async () => {
    setIsAdding(true);
    await addToCart(productId);
    setIsAdding(false);
  };
  
  return (
    <button onClick={handleClick} disabled={isAdding}>
      {isAdding ? 'Adding...' : 'Add to Cart'}
    </button>
  );
}
```

**Streaming with Suspense:**

```jsx
// app/dashboard/page.tsx
import { Suspense } from 'react';

export default function Dashboard() {
  return (
    <div>
      <h1>Dashboard</h1>
      
      {/* Stream different parts independently */}
      <Suspense fallback={<Skeleton />}>
        <UserStats /> {/* Loads first */}
      </Suspense>
      
      <Suspense fallback={<Skeleton />}>
        <RecentOrders /> {/* Loads independently */}
      </Suspense>
      
      <Suspense fallback={<Skeleton />}>
        <Analytics /> {/* Loads last */}
      </Suspense>
    </div>
  );
}

async function UserStats() {
  const stats = await fetchUserStats(); // Slow query
  return <StatsCard data={stats} />;
}
```

**Interview Tip:** Server Components eliminate the need for API routes and reduce client-side JavaScript significantly.

---

### Q3: Explain React Server Actions. Provide a real-world example.

**Answer:**

**Server Actions (React 19):**
- Server-side functions callable from client
- No API routes needed
- Progressive enhancement
- Built-in form handling

**Example:**

```jsx
// app/actions.ts (Server Actions)
'use server';

import { db } from '@/lib/database';
import { revalidatePath } from 'next/cache';

export async function createProduct(formData: FormData) {
  const name = formData.get('name') as string;
  const price = parseFloat(formData.get('price') as string);
  
  // Validation
  if (!name || price <= 0) {
    return { error: 'Invalid input' };
  }
  
  // Direct database operation
  const product = await db.product.create({
    data: { name, price }
  });
  
  // Revalidate cache
  revalidatePath('/products');
  
  return { success: true, product };
}

export async function updateProduct(id: string, formData: FormData) {
  const name = formData.get('name') as string;
  const price = parseFloat(formData.get('price') as string);
  
  await db.product.update({
    where: { id },
    data: { name, price }
  });
  
  revalidatePath(`/products/${id}`);
  return { success: true };
}

export async function deleteProduct(id: string) {
  await db.product.delete({ where: { id } });
  revalidatePath('/products');
  return { success: true };
}

// app/products/new/page.tsx (Client Component using Server Action)
'use client';

import { createProduct } from '@/app/actions';
import { useFormStatus, useFormState } from 'react-dom';

function SubmitButton() {
  const { pending } = useFormStatus();
  
  return (
    <button type="submit" disabled={pending}>
      {pending ? 'Creating...' : 'Create Product'}
    </button>
  );
}

export default function NewProductPage() {
  const [state, formAction] = useFormState(createProduct, null);
  
  return (
    <form action={formAction}>
      <input name="name" placeholder="Product name" required />
      <input name="price" type="number" placeholder="Price" required />
      
      {state?.error && <p className="error">{state.error}</p>}
      {state?.success && <p className="success">Product created!</p>}
      
      <SubmitButton />
    </form>
  );
}

// Optimistic Updates with Server Actions
'use client';

import { useOptimistic } from 'react';
import { likePost } from '@/app/actions';

export function LikeButton({ postId, initialLikes }) {
  const [optimisticLikes, addOptimisticLike] = useOptimistic(
    initialLikes,
    (state, amount) => state + amount
  );
  
  const handleLike = async () => {
    addOptimisticLike(1); // Immediate UI update
    await likePost(postId); // Server update
  };
  
  return (
    <button onClick={handleLike}>
      ❤️ {optimisticLikes}
    </button>
  );
}
```

**Interview Tip:** Server Actions simplify full-stack development by eliminating the need for separate API routes.

---

## 3. Hooks Deep Dive

### Q4: Explain useCallback vs useMemo. When should you use each?

**Answer:**

**useCallback:**
- Memoizes function reference
- Prevents function recreation
- Used for callbacks passed to child components

**useMemo:**
- Memoizes computed value
- Prevents expensive recalculations
- Used for derived data

**Example:**

```jsx
import { useState, useCallback, useMemo, memo } from 'react';

function ProductList() {
  const [products, setProducts] = useState([]);
  const [filter, setFilter] = useState('');
  const [sortBy, setSortBy] = useState('name');
  
  // useMemo - Expensive computation
  const filteredProducts = useMemo(() => {
    console.log('Filtering products...');
    return products
      .filter(p => p.name.toLowerCase().includes(filter.toLowerCase()))
      .sort((a, b) => a[sortBy].localeCompare(b[sortBy]));
  }, [products, filter, sortBy]); // Only recompute when these change
  
  // useCallback - Stable function reference
  const handleAddToCart = useCallback((productId) => {
    console.log('Adding to cart:', productId);
    // API call
    addToCart(productId);
  }, []); // Function never changes
  
  const handleRemove = useCallback((productId) => {
    setProducts(prev => prev.filter(p => p.id !== productId));
  }, []); // Stable reference
  
  return (
    <div>
      <input 
        value={filter}
        onChange={(e) => setFilter(e.target.value)}
        placeholder="Filter products..."
      />
      
      {filteredProducts.map(product => (
        <ProductCard
          key={product.id}
          product={product}
          onAddToCart={handleAddToCart} // Stable reference prevents re-render
          onRemove={handleRemove}
        />
      ))}
    </div>
  );
}

// Memoized child component
const ProductCard = memo(({ product, onAddToCart, onRemove }) => {
  console.log('Rendering ProductCard:', product.id);
  
  return (
    <div>
      <h3>{product.name}</h3>
      <button onClick={() => onAddToCart(product.id)}>
        Add to Cart
      </button>
      <button onClick={() => onRemove(product.id)}>
        Remove
      </button>
    </div>
  );
});
```

**Custom Hook with useCallback:**

```jsx
function useDebounce(callback, delay) {
  const callbackRef = useRef(callback);
  
  useEffect(() => {
    callbackRef.current = callback;
  }, [callback]);
  
  return useCallback(
    debounce((...args) => callbackRef.current(...args), delay),
    [delay]
  );
}

// Usage
function SearchComponent() {
  const [query, setQuery] = useState('');
  
  const debouncedSearch = useDebounce((value) => {
    console.log('Searching for:', value);
    // API call
  }, 500);
  
  const handleChange = (e) => {
    const value = e.target.value;
    setQuery(value);
    debouncedSearch(value);
  };
  
  return <input value={query} onChange={handleChange} />;
}
```

**Interview Tip:** Don't overuse these hooks. Only use when you have performance issues or need stable references.

---

## 4. Performance Optimization

### Q5: How would you optimize a large list rendering in React? Explain virtualization.

**Answer:**

**Virtualization:**
- Only render visible items
- Dramatically reduces DOM nodes
- Improves scroll performance
- Use libraries: react-window, react-virtualized

**Example:**

```jsx
import { FixedSizeList } from 'react-window';
import AutoSizer from 'react-virtualized-auto-sizer';

// Without Virtualization (BAD for large lists)
function SlowList({ items }) {
  return (
    <div>
      {items.map(item => (
        <div key={item.id} style={{ height: 50 }}>
          {item.name}
        </div>
      ))}
    </div>
  );
}

// With Virtualization (GOOD)
function VirtualizedList({ items }) {
  const Row = ({ index, style }) => (
    <div style={style}>
      <ProductCard product={items[index]} />
    </div>
  );
  
  return (
    <AutoSizer>
      {({ height, width }) => (
        <FixedSizeList
          height={height}
          width={width}
          itemCount={items.length}
          itemSize={100}
        >
          {Row}
        </FixedSizeList>
      )}
    </AutoSizer>
  );
}

// Variable Size List
import { VariableSizeList } from 'react-window';

function DynamicList({ items }) {
  const listRef = useRef();
  const rowHeights = useRef({});
  
  const getRowHeight = (index) => {
    return rowHeights.current[index] || 80;
  };
  
  const setRowHeight = (index, size) => {
    listRef.current.resetAfterIndex(0);
    rowHeights.current = { ...rowHeights.current, [index]: size };
  };
  
  const Row = ({ index, style }) => {
    const rowRef = useRef();
    
    useEffect(() => {
      if (rowRef.current) {
        setRowHeight(index, rowRef.current.clientHeight);
      }
    }, [index]);
    
    return (
      <div style={style}>
        <div ref={rowRef}>
          <ComplexCard data={items[index]} />
        </div>
      </div>
    );
  };
  
  return (
    <VariableSizeList
      ref={listRef}
      height={600}
      width="100%"
      itemCount={items.length}
      itemSize={getRowHeight}
    >
      {Row}
    </VariableSizeList>
  );
}
```

**Infinite Scroll with Virtualization:**

```jsx
import { useInfiniteQuery } from '@tanstack/react-query';
import { useVirtualizer } from '@tanstack/react-virtual';

function InfiniteList() {
  const {
    data,
    fetchNextPage,
    hasNextPage,
    isFetchingNextPage
  } = useInfiniteQuery({
    queryKey: ['products'],
    queryFn: ({ pageParam = 0 }) => fetchProducts(pageParam),
    getNextPageParam: (lastPage, pages) => lastPage.nextCursor,
  });
  
  const allRows = data?.pages.flatMap(page => page.items) ?? [];
  
  const parentRef = useRef();
  
  const virtualizer = useVirtualizer({
    count: hasNextPage ? allRows.length + 1 : allRows.length,
    getScrollElement: () => parentRef.current,
    estimateSize: () => 100,
    overscan: 5,
  });
  
  useEffect(() => {
    const [lastItem] = [...virtualizer.getVirtualItems()].reverse();
    
    if (!lastItem) return;
    
    if (
      lastItem.index >= allRows.length - 1 &&
      hasNextPage &&
      !isFetchingNextPage
    ) {
      fetchNextPage();
    }
  }, [
    hasNextPage,
    fetchNextPage,
    allRows.length,
    isFetchingNextPage,
    virtualizer.getVirtualItems(),
  ]);
  
  return (
    <div ref={parentRef} style={{ height: '600px', overflow: 'auto' }}>
      <div
        style={{
          height: `${virtualizer.getTotalSize()}px`,
          position: 'relative',
        }}
      >
        {virtualizer.getVirtualItems().map(virtualRow => {
          const isLoaderRow = virtualRow.index > allRows.length - 1;
          const item = allRows[virtualRow.index];
          
          return (
            <div
              key={virtualRow.index}
              style={{
                position: 'absolute',
                top: 0,
                left: 0,
                width: '100%',
                height: `${virtualRow.size}px`,
                transform: `translateY(${virtualRow.start}px)`,
              }}
            >
              {isLoaderRow ? (
                hasNextPage ? 'Loading...' : 'Nothing more to load'
              ) : (
                <ProductCard product={item} />
              )}
            </div>
          );
        })}
      </div>
    </div>
  );
}
```

**Interview Tip:** Mention that virtualization is essential for lists with 1000+ items. Also discuss windowing and lazy loading strategies.

---

## 5. State Management

### Q6: Compare Redux, Zustand, and Jotai. When would you use each?

**Answer:**

| Feature | Redux | Zustand | Jotai |
|---------|-------|---------|-------|
| **Boilerplate** | High | Low | Minimal |
| **Learning Curve** | Steep | Easy | Easy |
| **DevTools** | Excellent | Good | Good |
| **Bundle Size** | Large | Small | Tiny |
| **Use Case** | Large apps | Medium apps | Small-medium apps |

**Redux Toolkit (Modern Redux):**

```jsx
// store/slices/cartSlice.ts
import { createSlice, createAsyncThunk } from '@reduxjs/toolkit';

export const fetchCart = createAsyncThunk(
  'cart/fetchCart',
  async (userId) => {
    const response = await api.getCart(userId);
    return response.data;
  }
);

const cartSlice = createSlice({
  name: 'cart',
  initialState: {
    items: [],
    total: 0,
    status: 'idle',
  },
  reducers: {
    addItem: (state, action) => {
      state.items.push(action.payload);
      state.total += action.payload.price;
    },
    removeItem: (state, action) => {
      const index = state.items.findIndex(item => item.id === action.payload);
      if (index !== -1) {
        state.total -= state.items[index].price;
        state.items.splice(index, 1);
      }
    },
  },
  extraReducers: (builder) => {
    builder
      .addCase(fetchCart.pending, (state) => {
        state.status = 'loading';
      })
      .addCase(fetchCart.fulfilled, (state, action) => {
        state.status = 'succeeded';
        state.items = action.payload.items;
        state.total = action.payload.total;
      });
  },
});

export const { addItem, removeItem } = cartSlice.actions;
export default cartSlice.reducer;

// Component
function Cart() {
  const dispatch = useDispatch();
  const { items, total, status } = useSelector(state => state.cart);
  
  useEffect(() => {
    dispatch(fetchCart(userId));
  }, []);
  
  return (
    <div>
      {items.map(item => (
        <CartItem 
          key={item.id} 
          item={item}
          onRemove={() => dispatch(removeItem(item.id))}
        />
      ))}
      <Total amount={total} />
    </div>
  );
}
```

**Zustand (Simpler Alternative):**

```jsx
// store/cartStore.ts
import create from 'zustand';
import { devtools, persist } from 'zustand/middleware';

export const useCartStore = create(
  devtools(
    persist(
      (set, get) => ({
        items: [],
        total: 0,
        
        addItem: (item) => set((state) => ({
          items: [...state.items, item],
          total: state.total + item.price,
        })),
        
        removeItem: (id) => set((state) => {
          const item = state.items.find(i => i.id === id);
          return {
            items: state.items.filter(i => i.id !== id),
            total: state.total - (item?.price || 0),
          };
        }),
        
        clearCart: () => set({ items: [], total: 0 }),
        
        fetchCart: async (userId) => {
          const response = await api.getCart(userId);
          set({ items: response.items, total: response.total });
        },
      }),
      { name: 'cart-storage' }
    )
  )
);

// Component
function Cart() {
  const { items, total, removeItem, fetchCart } = useCartStore();
  
  useEffect(() => {
    fetchCart(userId);
  }, []);
  
  return (
    <div>
      {items.map(item => (
        <CartItem 
          key={item.id} 
          item={item}
          onRemove={() => removeItem(item.id)}
        />
      ))}
      <Total amount={total} />
    </div>
  );
}
```

**Jotai (Atomic State):**

```jsx
// store/atoms.ts
import { atom } from 'jotai';
import { atomWithStorage } from 'jotai/utils';

export const cartItemsAtom = atomWithStorage('cart-items', []);

export const cartTotalAtom = atom((get) => {
  const items = get(cartItemsAtom);
  return items.reduce((sum, item) => sum + item.price, 0);
});

export const addItemAtom = atom(
  null,
  (get, set, item) => {
    const items = get(cartItemsAtom);
    set(cartItemsAtom, [...items, item]);
  }
);

export const removeItemAtom = atom(
  null,
  (get, set, id) => {
    const items = get(cartItemsAtom);
    set(cartItemsAtom, items.filter(item => item.id !== id));
  }
);

// Component
import { useAtom, useAtomValue, useSetAtom } from 'jotai';

function Cart() {
  const items = useAtomValue(cartItemsAtom);
  const total = useAtomValue(cartTotalAtom);
  const removeItem = useSetAtom(removeItemAtom);
  
  return (
    <div>
      {items.map(item => (
        <CartItem 
          key={item.id} 
          item={item}
          onRemove={() => removeItem(item.id)}
        />
      ))}
      <Total amount={total} />
    </div>
  );
}
```

**Interview Tip:** 
- **Redux**: Large apps, complex state, time-travel debugging
- **Zustand**: Medium apps, simpler API, less boilerplate
- **Jotai**: Small-medium apps, atomic updates, minimal bundle size

---

## 🎯 Quick Reference

### Top 10 Must-Know Topics

1. ✅ React 18 Concurrent Features
2. ✅ React 19 Server Components
3. ✅ Server Actions
4. ✅ useCallback vs useMemo
5. ✅ Virtualization for large lists
6. ✅ State Management (Redux/Zustand/Jotai)
7. ✅ Performance optimization
8. ✅ Testing (Jest, React Testing Library)
9. ✅ Error Boundaries
10. ✅ Custom Hooks patterns

### Latest Trends (2024-2026)

- **React 19** - Server Components, Server Actions
- **Next.js 15** - Turbopack, Partial Prerendering
- **TanStack Query v5** - Better data fetching
- **Remix** - Full-stack React framework
- **React Compiler** - Auto-optimization (experimental)

---

**Next:** [System Design Mastery](../../02-system-design/README.md)
