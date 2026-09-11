import React, { useState, useEffect } from "react";
import { Link } from "react-router-dom";

import "../styles/Navbar.css";

import Logo from "../assets/icons/logoBaner.png";
import MenuIcon from "../assets/icons/menu.svg";
import closeMenuIcon from "../assets/icons/closeMenu.svg";
import home from "../assets/icons/home.svg";
import signup from "../assets/icons/signup.svg";
import login from "../assets/icons/login.svg";
import logout from "../assets/icons/logout.svg";
import account from "../assets/icons/account.svg";
import dashboard from "../assets/icons/dashboard.svg";

import IRAN_FLAG from "../assets/icons/iran.svg";
import USA_FLAG from "../assets/icons/usa.webp";


const LANGUAGES = [
  { code: "fa", label: "Farsi", shortLabel: "FA", flag: IRAN_FLAG },
  { code: "fa-fun", label: "Fun Farsi", shortLabel: "FA-Fun", flag: IRAN_FLAG },
  { code: "en", label: "English", shortLabel: "EN", flag: USA_FLAG },
];

function Navbar() {
  const [lang, setLang] = useState(() => {
    return localStorage.getItem("app_lang") || "fa";
  });

  // Separate states for the side menu and the language dropdown
  const [isMenuOpen, setIsMenuOpen] = useState(false);
  const [isLangOpen, setIsLangOpen] = useState(false);

  // User authentication check (from your old menu)
  const userInfo = JSON.parse(localStorage.getItem("user_info"));
  const hasAccount = userInfo !== null;

  const currentLangObj = LANGUAGES.find((item) => item.code === lang) || LANGUAGES[0];

  const handleSelectLanguage = (selectedLangCode) => {
    setLang(selectedLangCode);
    localStorage.setItem("app_lang", selectedLangCode);
    setIsLangOpen(false);

    window.dispatchEvent(new Event("storage"));
    window.dispatchEvent(new Event("languageChange"));
  };

  useEffect(() => {
    localStorage.setItem("app_lang", lang);
    document.documentElement.lang = lang;
  }, [lang]);

  return (
    <>
      {/* Sliding Side Menu */}
      <menu className={isMenuOpen ? "open" : ""}>
        <img 
          className="Logo close" 
          src={closeMenuIcon} 
          alt="Close Menu" 
          onClick={() => setIsMenuOpen(false)} 
        />
        <ol>
          <li>
            <Link className="menu-link" to="/" onClick={() => setIsMenuOpen(false)}>   
                  <img
                    src={home}
                    className="icon" 
                    alt="Icon" 
                  />
              <p>Home</p>
            </Link>
          </li>


          {/* Render if user is NOT logged in */}
          {!hasAccount && (
            <>
              <div className="line"></div>
              <li>
                <Link className="menu-link" to="/login" onClick={() => setIsMenuOpen(false)}>   
                  <img
                    src={login}
                    className="icon" 
                    alt="Icon" 
                  />
                  <p>Login</p>
                </Link>
              </li>
              <div className="line"></div>

              <li>
                <Link className="menu-link" to="/signup" onClick={() => setIsMenuOpen(false)}>   
                  <img
                    src={signup}
                    className="icon" 
                    alt="Icon" 
                  />
                  <p>Signup</p>
                </Link>
              </li>

            </>
          )}

          {/* Render if user IS logged in */}
          {hasAccount && (
            <>
            <div className="line"></div>
              <li>
                <Link className="menu-link" to="/dashboard" onClick={() => setIsMenuOpen(false)}>   
                  <img
                    src={dashboard}
                    className="icon" 
                    alt="Icon" 
                  />
                  <p>Dashboard</p>
                </Link>
              </li>
              <div className="line"></div>

              <li>
                <Link className="menu-link" to="/account" onClick={() => setIsMenuOpen(false)}>   
                  <img
                    src={account}
                    className="icon" 
                    alt="Icon" 
                  />
                  <p>Account</p>
                </Link>
              </li>
              <div className="line"></div>

              <li>
                <Link className="menu-link" to="/logout" onClick={() => setIsMenuOpen(false)}>   
                  <img
                    src={logout}
                    className="icon" 
                    alt="Icon" 
                  />
                  <p>Logout</p>
                </Link>
              </li>
            </>
          )}

          {/* <li>
            <Link className="menu-link" to="/about" onClick={() => setIsMenuOpen(false)}>   
              <p>About us</p>
            </Link>
          </li> */}
        </ol>
      </menu>

      {/* Main Top Navbar */}
      <nav className="navbar">
        <img className="Logo" src={Logo} alt="Logo" />
        
        <div className="right-nav">
          <div className="lang-selector">
            <button 
              className="lang-btn" 
              onClick={() => setIsLangOpen(!isLangOpen)}
              type="button"
            >
              <img 
                src={currentLangObj.flag} 
                alt={currentLangObj.label} 
                className="flag-icon"
              />
              <span className="lang-text">{currentLangObj.shortLabel}</span>
            </button>

            {isLangOpen && (
              <div className="lang-dropdown">
                {LANGUAGES.map((item) => (
                  <div 
                    key={item.code}
                    className={`lang-option ${lang === item.code ? "active" : ""}`}
                    onClick={() => handleSelectLanguage(item.code)}
                  >
                    <img src={item.flag} alt={item.label} className="flag-icon" />
                    <span>{item.label}</span>
                  </div>
                ))}
              </div>
            )}
          </div>

          <img 
            className="Logo" 
            src={MenuIcon} 
            alt="Open Menu" 
            onClick={() => setIsMenuOpen(true)} 
          />
        </div>
      </nav>
    </>
  );
}

export default Navbar;