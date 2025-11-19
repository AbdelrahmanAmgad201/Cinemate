import { useState } from 'react'
import { BrowserRouter } from 'react-router-dom';
import React, { Suspense } from 'react';
import AppRoutes from './routes/AppRoutes';
import LoadingFallback from './components/LoadingFallback';




function App() {
  const [count, setCount] = useState(0)

  return (

    <BrowserRouter>
        {/*<Suspense> lets you display a fallback until its children have finished loading.*/}
        <Suspense fallback={<LoadingFallback />}>
            <AppRoutes />
        </Suspense>
    </BrowserRouter>
  )
}

export default App


