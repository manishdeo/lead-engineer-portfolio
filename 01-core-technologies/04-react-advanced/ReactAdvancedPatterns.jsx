// Custom Hooks - Interview Favorite
import { useState, useEffect, useCallback, useMemo, useRef } from 'react';

// 1. Custom Hook for API calls with loading, error states
export const useApi = (url, options = {}) => {
  const [data, setData] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  
  const fetchData = useCallback(async () => {
    try {
      setLoading(true);
      setError(null);
      const response = await fetch(url, options);
      if (!response.ok) {
        throw new Error(`HTTP error! status: ${response.status}`);
      }
      const result = await response.json();
      setData(result);
    } catch (err) {
      setError(err.message);
    } finally {
      setLoading(false);
    }
  }, [url, JSON.stringify(options)]);
  
  useEffect(() => {
    fetchData();
  }, [fetchData]);
  
  return { data, loading, error, refetch: fetchData };
};

// 2. Custom Hook for Local Storage with sync across tabs
export const useLocalStorage = (key, initialValue) => {
  const [storedValue, setStoredValue] = useState(() => {
    try {
      const item = window.localStorage.getItem(key);
      return item ? JSON.parse(item) : initialValue;
    } catch (error) {
      console.error(`Error reading localStorage key "${key}":`, error);
      return initialValue;
    }
  });
  
  const setValue = useCallback((value) => {
    try {
      const valueToStore = value instanceof Function ? value(storedValue) : value;
      setStoredValue(valueToStore);
      window.localStorage.setItem(key, JSON.stringify(valueToStore));
      
      // Dispatch custom event for cross-tab sync
      window.dispatchEvent(new CustomEvent('localStorage', {
        detail: { key, newValue: valueToStore }
      }));
    } catch (error) {
      console.error(`Error setting localStorage key "${key}":`, error);
    }
  }, [key, storedValue]);
  
  // Listen for changes from other tabs
  useEffect(() => {
    const handleStorageChange = (e) => {
      if (e.detail.key === key) {
        setStoredValue(e.detail.newValue);
      }
    };
    
    window.addEventListener('localStorage', handleStorageChange);
    return () => window.removeEventListener('localStorage', handleStorageChange);
  }, [key]);
  
  return [storedValue, setValue];
};

// 3. Custom Hook for Debounced Values
export const useDebounce = (value, delay) => {
  const [debouncedValue, setDebouncedValue] = useState(value);
  
  useEffect(() => {
    const handler = setTimeout(() => {
      setDebouncedValue(value);
    }, delay);
    
    return () => {
      clearTimeout(handler);
    };
  }, [value, delay]);
  
  return debouncedValue;
};

// 4. Custom Hook for Previous Value
export const usePrevious = (value) => {
  const ref = useRef();
  useEffect(() => {
    ref.current = value;
  });
  return ref.current;
};

// 5. Higher-Order Component for Error Boundary
import React from 'react';

export const withErrorBoundary = (WrappedComponent, ErrorFallback) => {
  return class extends React.Component {
    constructor(props) {
      super(props);
      this.state = { hasError: false, error: null };
    }
    
    static getDerivedStateFromError(error) {
      return { hasError: true, error };
    }
    
    componentDidCatch(error, errorInfo) {
      console.error('Error caught by boundary:', error, errorInfo);
      // Log to error reporting service
    }
    
    render() {
      if (this.state.hasError) {
        return <ErrorFallback error={this.state.error} />;
      }
      
      return <WrappedComponent {...this.props} />;
    }
  };
};

// 6. React Context with useReducer - State Management Pattern
import { createContext, useContext, useReducer } from 'react';

// Actions
const ACTIONS = {
  SET_LOADING: 'SET_LOADING',
  SET_USER: 'SET_USER',
  SET_ERROR: 'SET_ERROR',
  LOGOUT: 'LOGOUT'
};

// Reducer
const authReducer = (state, action) => {
  switch (action.type) {
    case ACTIONS.SET_LOADING:
      return { ...state, loading: action.payload };
    case ACTIONS.SET_USER:
      return { ...state, user: action.payload, loading: false, error: null };
    case ACTIONS.SET_ERROR:
      return { ...state, error: action.payload, loading: false };
    case ACTIONS.LOGOUT:
      return { user: null, loading: false, error: null };
    default:
      return state;
  }
};

// Context
const AuthContext = createContext();

// Provider Component
export const AuthProvider = ({ children }) => {
  const [state, dispatch] = useReducer(authReducer, {
    user: null,
    loading: false,
    error: null
  });
  
  const login = async (credentials) => {
    dispatch({ type: ACTIONS.SET_LOADING, payload: true });
    try {
      const response = await fetch('/api/auth/login', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(credentials)
      });
      
      if (!response.ok) throw new Error('Login failed');
      
      const user = await response.json();
      dispatch({ type: ACTIONS.SET_USER, payload: user });
    } catch (error) {
      dispatch({ type: ACTIONS.SET_ERROR, payload: error.message });
    }
  };
  
  const logout = () => {
    dispatch({ type: ACTIONS.LOGOUT });
  };
  
  return (
    <AuthContext.Provider value={{ ...state, login, logout }}>
      {children}
    </AuthContext.Provider>
  );
};

// Custom hook to use auth context
export const useAuth = () => {
  const context = useContext(AuthContext);
  if (!context) {
    throw new Error('useAuth must be used within AuthProvider');
  }
  return context;
};

// 7. Performance Optimized Component with React.memo
import React, { memo, useMemo, useCallback } from 'react';

const ExpensiveListItem = memo(({ item, onUpdate, onDelete }) => {
  // Expensive calculation
  const processedData = useMemo(() => {
    return item.data.map(d => d * 2).filter(d => d > 10);
  }, [item.data]);
  
  const handleUpdate = useCallback(() => {
    onUpdate(item.id);
  }, [item.id, onUpdate]);
  
  const handleDelete = useCallback(() => {
    onDelete(item.id);
  }, [item.id, onDelete]);
  
  return (
    <div className="list-item">
      <h3>{item.title}</h3>
      <p>Processed count: {processedData.length}</p>
      <button onClick={handleUpdate}>Update</button>
      <button onClick={handleDelete}>Delete</button>
    </div>
  );
});

// 8. Custom Hook for Infinite Scroll
export const useInfiniteScroll = (fetchMore, hasMore) => {
  const [loading, setLoading] = useState(false);
  
  useEffect(() => {
    const handleScroll = async () => {
      if (
        window.innerHeight + document.documentElement.scrollTop
        !== document.documentElement.offsetHeight
        || loading
        || !hasMore
      ) {
        return;
      }
      
      setLoading(true);
      await fetchMore();
      setLoading(false);
    };
    
    window.addEventListener('scroll', handleScroll);
    return () => window.removeEventListener('scroll', handleScroll);
  }, [fetchMore, hasMore, loading]);
  
  return loading;
};

// 9. Compound Component Pattern
const Tabs = ({ children, defaultTab = 0 }) => {
  const [activeTab, setActiveTab] = useState(defaultTab);
  
  return (
    <div className="tabs">
      {React.Children.map(children, (child, index) =>
        React.cloneElement(child, { activeTab, setActiveTab, index })
      )}
    </div>
  );
};

const TabList = ({ children, activeTab, setActiveTab }) => (
  <div className="tab-list">
    {React.Children.map(children, (child, index) =>
      React.cloneElement(child, { 
        isActive: activeTab === index,
        onClick: () => setActiveTab(index)
      })
    )}
  </div>
);

const Tab = ({ children, isActive, onClick }) => (
  <button 
    className={`tab ${isActive ? 'active' : ''}`}
    onClick={onClick}
  >
    {children}
  </button>
);

const TabPanels = ({ children, activeTab }) => (
  <div className="tab-panels">
    {React.Children.toArray(children)[activeTab]}
  </div>
);

const TabPanel = ({ children }) => (
  <div className="tab-panel">{children}</div>
);

// Usage
const App = () => (
  <Tabs defaultTab={0}>
    <TabList>
      <Tab>Tab 1</Tab>
      <Tab>Tab 2</Tab>
      <Tab>Tab 3</Tab>
    </TabList>
    <TabPanels>
      <TabPanel>Content 1</TabPanel>
      <TabPanel>Content 2</TabPanel>
      <TabPanel>Content 3</TabPanel>
    </TabPanels>
  </Tabs>
);

// 10. Custom Hook for WebSocket
export const useWebSocket = (url) => {
  const [socket, setSocket] = useState(null);
  const [lastMessage, setLastMessage] = useState(null);
  const [readyState, setReadyState] = useState(WebSocket.CONNECTING);
  
  useEffect(() => {
    const ws = new WebSocket(url);
    
    ws.onopen = () => setReadyState(WebSocket.OPEN);
    ws.onclose = () => setReadyState(WebSocket.CLOSED);
    ws.onerror = () => setReadyState(WebSocket.CLOSED);
    ws.onmessage = (event) => {
      setLastMessage(JSON.parse(event.data));
    };
    
    setSocket(ws);
    
    return () => {
      ws.close();
    };
  }, [url]);
  
  const sendMessage = useCallback((message) => {
    if (socket && readyState === WebSocket.OPEN) {
      socket.send(JSON.stringify(message));
    }
  }, [socket, readyState]);
  
  return { lastMessage, sendMessage, readyState };
};