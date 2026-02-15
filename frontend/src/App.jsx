import { useState } from 'react'
import { BrowserRouter } from 'react-router-dom';
import React, { Suspense } from 'react';
import AppRoutes from './routes/AppRoutes.jsx';
import LoadingFallback from './components/LoadingFallback.jsx';
import './App.css'
import AuthProvider from './context/AuthContext.jsx';

import {ToastProvider} from "./context/ToastContext.jsx";
import {WatchPartyProvider} from "./context/WatchPartyContext.jsx";

function App() {

  return (

    // ToastProvider provide a toast to use anywhere
    // AuthProvider provides some authentication-related context

    <ToastProvider>
        <AuthProvider>
            <WatchPartyProvider>
                <BrowserRouter>
                    {/*<Suspense> lets you display a fallback until its children have finished loading.*/}
                    <Suspense fallback={<LoadingFallback />}>
                        <AppRoutes />
                    </Suspense>
                </BrowserRouter>
            </WatchPartyProvider>
        </AuthProvider>
    </ToastProvider>
  )
}

export default App


