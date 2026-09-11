import Signup from "../../layouts/Signup";
import { Helmet } from "react-helmet-async";

function SignupPage() {
  return (
    <>
      <Helmet>
        <title>Amiro - Signup</title>
        <meta name="description" content="Amiro SPA - Signup" />
      </Helmet>
      <Signup />
    </>
  );
}

export default SignupPage;
