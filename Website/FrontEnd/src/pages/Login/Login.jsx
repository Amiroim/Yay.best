import Login from "../../layouts/Login"
import { Helmet } from "react-helmet-async";

function LoginPage() {
  return (
    <>
      <Helmet>
        <title>Yay - Login</title>
        <meta name="description" content="Amiro SPA - Login" />
      </Helmet>
      <Login />
    </>
  );
}

export default LoginPage;
