import Landing from "../../layouts/Landing";
import { Helmet } from "react-helmet-async";

function LandingPage() {
  return (
    <>
      <Helmet>
        <title>Amiro - Landing</title>
        <meta name="description" content="Amiro SPA" />
      </Helmet>
      <Landing />
    </>
  );
}

export default LandingPage;
