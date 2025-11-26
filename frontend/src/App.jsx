import { useState } from 'react'
import { BrowserRouter } from 'react-router-dom';
import React, { Suspense } from 'react';
import AppRoutes from './routes/appRoutes';
import LoadingFallback from './components/LoadingFallback';
import './App.css'
import AuthProvider from './context/authContext';

import {ErrorToastProvider} from "./context/errorToastContext.jsx";

function App() {

  return (
    // AuthProvider provides value={{ user, loading, signIn, signOut, isAuthenticated: !!user}}
    // to all the children, basically all the app
    <AuthProvider>
        {/*ErrorToastProvider provide an error toast to use anywhere*/}
        <ErrorToastProvider>
            <BrowserRouter>
                {/*<Suspense> lets you display a fallback until its children have finished loading.*/}
                <Suspense fallback={<LoadingFallback />}>
                    <AppRoutes />
                </Suspense>
            </BrowserRouter>
        </ErrorToastProvider>
    </AuthProvider>
  )
}

export default App


