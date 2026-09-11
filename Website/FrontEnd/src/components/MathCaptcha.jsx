import React, { useState, useEffect } from "react";
import translations from "../assets/translations.json"; 

import Renew from "../assets/icons/renew.svg";
import "../styles/MathCaptcha.css";

export default function MathCaptcha({ onVerify }) {
  const [lang, setLang] = useState(() => localStorage.getItem("app_lang") || "fa");
  const [num1, setNum1] = useState(0);
  const [num2, setNum2] = useState(0);
  const [userAnswer, setUserAnswer] = useState("");
  
  const [status, setStatus] = useState("neutral");

  const generateCaptcha = () => {
    const n1 = Math.floor(Math.random() * 9) + 1;
    const n2 = Math.floor(Math.random() * 9) + 1;
    setNum1(n1);
    setNum2(n2);
    setUserAnswer("");
    setStatus("neutral");
    if (onVerify) onVerify(false);
  };

  const t = (key) => {
    return translations[key]?.[lang] || key;
  };

  useEffect(() => {
    const handleLangChange = () => {
      setLang(localStorage.getItem("app_lang") || "fa");
    };

    window.addEventListener("storage", handleLangChange);
    window.addEventListener("languageChange", handleLangChange);

    return () => {
      window.removeEventListener("storage", handleLangChange);
      window.removeEventListener("languageChange", handleLangChange);
    };
  }, []);

  useEffect(() => {
    generateCaptcha();
  }, []);

  const handleChange = (e) => {
    const val = e.target.value;
    setUserAnswer(val);

    if (val === "") {
      setStatus("neutral");
      if (onVerify) onVerify(false);
      return;
    }

    const correctAnswer = num1 + num2;
    const parsedVal = parseInt(val, 10);

    if (parsedVal === correctAnswer) {
      setStatus("correct");
      if (onVerify) onVerify(true); 
    } else {
      setStatus("error");
      if (onVerify) onVerify(false);
    }
  };

  return (
    <div className="captcha">
      <div className="captcha-group">

      <p className="captcha-numbers">
        {num1} + {num2} = ?
      </p>
      <button
        type="button"
        onClick={generateCaptcha}
        className="captcha-button"
        title={t("captcha_renew")}
        >
        <img src={Renew} alt="" />
      </button>
        </div>

      <input
        type="number"
        maxLength={2}
        value={userAnswer}
        className={`captcha-input ${status === "correct" ? "is-valid" : ""} ${status === "error" ? "has-error" : ""}`}
        onChange={handleChange}
        placeholder={t("captcha_answer")}
      />
    </div>
  );
}