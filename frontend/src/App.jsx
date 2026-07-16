import { Suspense } from 'react';
import { BrowserRouter } from 'react-router-dom';
import AppRoutes from './routes/AppRoutes.jsx';
import LoadingFallback from './components/LoadingFallback.jsx';
import ErrorBoundary from './components/ErrorBoundary.jsx';
import AuthProvider from './context/AuthContext.jsx';

import { ToastProvider } from './context/ToastContext.jsx';
import { WatchPartyProvider } from './context/WatchPartyContext.jsx';

function App() {
    return (
        <ToastProvider>
            <AuthProvider>
                <WatchPartyProvider>
                    <BrowserRouter>
                        <ErrorBoundary>
                            <Suspense fallback={<LoadingFallback fullScreen />}>
                                <AppRoutes />
                            </Suspense>
                        </ErrorBoundary>
                    </BrowserRouter>
                </WatchPartyProvider>
            </AuthProvider>
        </ToastProvider>
    );
}

export default App;
