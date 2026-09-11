import Dashboard from "../../layouts/Dashboard"
import { Helmet } from "react-helmet-async";


function DashboardPage() {
  return (
    <>
      <Helmet>
        <title>Amiro - Dashboard</title>
        <meta name="description" content="Amiro SPA - Dashboard " />
      </Helmet>
      <Dashboard />
    </>
  );
}

export default DashboardPage;
