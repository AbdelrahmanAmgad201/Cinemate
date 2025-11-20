import { useState } from 'react'
import { BrowserRouter } from 'react-router-dom';
import React, { Suspense } from 'react';
import AppRoutes from './routes/AppRoutes';
import LoadingFallback from './components/LoadingFallback';
import './App.css'
import AuthProvider from './context/AuthContext';


function App() {

  return (
    // AuthProvider provides value={{ user, loading, signIn, signOut, isAuthenticated: !!user}}
    // to all the children, basically all the app
    <AuthProvider>
        <BrowserRouter>
            {/*<Suspense> lets you display a fallback until its children have finished loading.*/}
            <Suspense fallback={<LoadingFallback />}>
                <AppRoutes />
            </Suspense>
        </BrowserRouter>
    </AuthProvider>
  )
}

export default App


