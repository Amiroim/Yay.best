import { BrowserRouter, Routes, Route } from "react-router-dom";
import LandingPage from "../pages/Landing/Landing";
import DashboardPage from "../pages/Dashboard/Dashboard";
import AccountPage from "../pages/Account/Account";
import LoginPage from "../pages/Login/Login";
import SignupPage from "../pages/Signup/Signup";

function Router() {
  return (
    <BrowserRouter>
      <Routes>
        <Route path="/" element={<LandingPage />} />
        <Route path="/Dashboard" element={<DashboardPage/>}/>
        <Route path="/Account" element={<AccountPage/>}/>
        <Route path="/Login" element={<LoginPage/>}/>
        <Route path="/Signup" element={<SignupPage/>}/>
      </Routes>
    </BrowserRouter>
  );
}

export default Router;
