import "../styles/Login.css";

import Navbar from "../components/Navbar";
import Footer from "../components/Footer";
import AlertToast from "../components/AlertToast";
import MathCaptcha from "../components/MathCaptcha";

import React, { useState, useEffect } from 'react';
import { useSearchParams, useNavigate } from 'react-router-dom'; // اضافه شدن هوک‌های روتر
import translations from "../assets/translations.json"; 

function Login() {
  const [lang, setLang] = useState(() => localStorage.getItem("app_lang") || "fa");
  const [isCaptchaValid, setIsCaptchaValid] = useState(false);
  const [showError, setShowError] = useState(false);
  const [showSuccess, setShowSuccess] = useState(false);
  // const [showInfo, setShowInfo] = useState(false);

  // استیت برای ذخیره شماره موبایل
  const [phoneNumber, setPhoneNumber] = useState("");

  const [searchParams] = useSearchParams();
  const navigate = useNavigate();

  // گرفتن متد از URL
  const method = searchParams.get("method"); 
  const isPhoneMethod = method === "phone";

  const handleSubmit = (e) => {
    e.preventDefault();

    if (!isCaptchaValid) {
      setShowError(true);
      return;
    }

    if (isPhoneMethod) {
      // اگر متد شماره موبایل بود، بعد از تایید فرم ریدایرکت بشه
      // می‌تونید شماره رو هم در استیت پاس بدید تا در صفحه auth بهش دسترسی داشته باشید
      navigate("/auth?method=phone", { state: { phone: phoneNumber } });
    } else {
      // لاگین معمولی با ایمیل/نام کاربری و رمز عبور
      console.log("فرم با موفقیت ارسال شد");
      setShowSuccess(true);
      // منطق لاگین شما اینجا قرار می‌گیره
    }
  };

  const t = (key) => {
    return translations[key]?.[lang] || key;
  };

  useEffect(() => {
    const handleLangChange = () => {
      setLang(localStorage.getItem("app_lang") || "fa");
    };

    window.addEventListener("storage", handleLangChange);
    return () => window.removeEventListener("storage", handleLangChange);
  }, []);

  return (
    <>
      <Navbar />
      <form id="login" className="entrance-box" onSubmit={handleSubmit}>
        <div className="titles">
          <h1 className="title">{isPhoneMethod ? t("login_phone_title") : t("login_title")}</h1>
          <p className="subtitle">{isPhoneMethod ? t("login_phone_subtitle") : t("login_subtitle")}</p>
        </div>
        
        <div className="input-items">
          <div className="inputs">
            
            {/* رندر شرطی بر اساس متد موجود در URL */}
            {isPhoneMethod ? (
              <input 
                type="tel" 
                placeholder={t("login_phone_input")} // مثلا: "شماره موبایل خود را وارد کنید"
                value={phoneNumber}
                onChange={(e) => setPhoneNumber(e.target.value)}
                required
              />
            ) : (
              <>
                <input 
                  type="text" 
                  placeholder={t("login_username-email_input")} 
                  required
                />
                <input 
                  type="password" 
                  placeholder={t("login_password_input")} 
                  required
                />
              </>
            )}

            <MathCaptcha onVerify={(isValid) => setIsCaptchaValid(isValid)} />
          </div>

          <button className="login" type="submit">
            {isPhoneMethod ? t("login_phone_button") : t("login_button")}
          </button>
        </div>
      </form>
      
      <AlertToast
        isOpen={showError}
        type="error"
        title={t("alert_error")}
        message={t("captcha_error")}
        duration={4000}
        onClose={() => setShowError(false)}
      />
      <AlertToast
        isOpen={showSuccess}
        type="success"
        title={t("alert_success")}
        message={t("alert_login_success")}
        duration={4000}
        onClose={() => setShowSuccess(false)}
      />
      <Footer />
    </>
  );
}

export default Login;