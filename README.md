<div align="center">

# Yay.best

<p>
  <img src="https://img.shields.io/badge/Status-In%20Development-yellow?style=for-the-badge" alt="Status" />
  <img src="https://img.shields.io/badge/Platform-Minecraft-brightgreen?style=for-the-badge" alt="Minecraft" />
  <img src="https://img.shields.io/badge/Stack-React%20%7C%20Express%20%7C%20Java%20%7C%20SQL-blue?style=for-the-badge" alt="Stack" />
</p>

<p><em>A centralized player authentication and administration system connecting Minecraft servers to a React dashboard via an Express.js REST API.</em></p>

</div>

---

## Tech Stack

<table>
  <thead>
    <tr>
      <th align="left">Layer</th>
      <th align="left">Technology</th>
      <th align="left">Description</th>
    </tr>
  </thead>
  <tbody>
    <tr>
      <td><b>Plugin</b></td>
      <td><code>Java</code> (Spigot / Paper)</td>
      <td>Intercepts player logins and dispatches REST calls to the backend.</td>
    </tr>
    <tr>
      <td><b>Backend</b></td>
      <td><code>Node.js</code> / <code>Express.js</code></td>
      <td>Exposes endpoints, runs business logic, and interacts with the database.</td>
    </tr>
    <tr>
      <td><b>Frontend</b></td>
      <td><code>React.js</code></td>
      <td>Web management dashboard for player accounts, roles, and moderation.</td>
    </tr>
    <tr>
      <td><b>Database</b></td>
      <td><code>SQL</code> (MySQL / PostgreSQL)</td>
      <td>Stores user profiles, credentials, ban entries, and session logs.</td>
    </tr>
  </tbody>
</table>

---

## Features & Responsibilities

<table>
  <thead>
    <tr>
      <th align="left">Feature</th>
      <th align="center">Minecraft Plugin</th>
      <th align="center">Express Backend</th>
      <th align="center">React Panel</th>
    </tr>
  </thead>
  <tbody>
    <tr>
      <td>User Registration</td>
      <td align="center">❌</td>
      <td align="center">✅</td>
      <td align="center">✅</td>
    </tr>
    <tr>
      <td>Join Verification (Ban/Auth Check)</td>
      <td align="center">✅</td>
      <td align="center">✅</td>
      <td align="center">❌</td>
    </tr>
    <tr>
      <td>Account Management (Edit / Delete)</td>
      <td align="center">❌</td>
      <td align="center">✅</td>
      <td align="center">✅</td>
    </tr>
    <tr>
      <td>Ban Moderation Console</td>
      <td align="center">❌</td>
      <td align="center">✅</td>
      <td align="center">✅</td>
    </tr>
    <tr>
      <td>Session & Activity Logging</td>
      <td align="center">✅</td>
      <td align="center">✅</td>
      <td align="center">✅</td>
    </tr>
  </tbody>
</table>

---

## API Blueprint

<table>
  <thead>
    <tr>
      <th align="left">Method</th>
      <th align="left">Endpoint</th>
      <th align="left">Purpose</th>
    </tr>
  </thead>
  <tbody>
    <tr>
      <td><code>POST</code></td>
      <td><code>/api/auth/register</code></td>
      <td>Create new web and game user accounts</td>
    </tr>
    <tr>
      <td><code>POST</code></td>
      <td><code>/api/auth/login</code></td>
      <td>Authenticate dashboard users</td>
    </tr>
    <tr>
      <td><code>POST</code></td>
      <td><code>/api/player/verify</code></td>
      <td>Plugin handshake to validate player status on join</td>
    </tr>
    <tr>
      <td><code>GET</code></td>
      <td><code>/api/player/ban-status/:uuid</code></td>
      <td>Check active ban records by Minecraft UUID</td>
    </tr>
    <tr>
      <td><code>GET</code></td>
      <td><code>/api/admin/players</code></td>
      <td>Retrieve a paginated list of accounts</td>
    </tr>
    <tr>
      <td><code>POST</code></td>
      <td><code>/api/admin/bans</code></td>
      <td>Apply temporary or permanent player bans</td>
    </tr>
  </tbody>
</table>

---

## 🚀 Development Roadmap

<table>
  <thead>
    <tr>
      <th align="left">Milestone</th>
      <th align="center">Progress</th>
    </tr>
  </thead>
  <tbody>
    <tr>
      <td>Plugin-to-API HTTP event listeners</td>
      <td align="center">🟡 In Progress</td>
    </tr>
    <tr>
      <td>JWT auth & API security middleware</td>
      <td align="center">🟡 In Progress</td>
    </tr>
    <tr>
      <td>SQL schema setup and migrations</td>
      <td align="center">⚪ Pending</td>
    </tr>
    <tr>
      <td>React dashboard integration</td>
      <td align="center">⚪ Pending</td>
    </tr>
    <tr>
      <td>Public repository release</td>
      <td align="center">⚪ Pending</td>
    </tr>
  </tbody>
</table>
