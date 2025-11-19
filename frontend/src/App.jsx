import { useState } from 'react'
import './App.css'
import { BrowserRouter as Router, Routes, Route, Link } from "react-router-dom";
import UserRoutes from './routes/userRoutes';
import OrgRoutes from './routes/orgRoutes';
import AdminRoutes from './routes/adminRoutes';



function App() {
  const [count, setCount] = useState(0)

  return (
    <Router>
      <Routes>
        {UserRoutes()}
        {OrgRoutes()}
        {AdminRoutes()}
      </Routes>
    </Router>
  )
}

export default App


