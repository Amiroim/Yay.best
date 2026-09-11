import Account from "../../layouts/Account"
import { Helmet } from "react-helmet-async";


function AccountPage() {
  return (
    <>
      <Helmet>
        <title>Amiro - Account</title>
        <meta name="description" content="Amiro SPA - Account" />
      </Helmet>
      <Account />
    </>
  );
}

export default AccountPage;
