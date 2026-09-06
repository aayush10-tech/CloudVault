import React, { useEffect, useState } from "react";
import { createRoot } from "react-dom/client";
import {
  Folder,
  Upload,
  Search,
  Star,
  Trash2,
  Home,
  Share2,
  Download,
  Plus,
  LogOut,
  Grid2X2,
  List,
  MoreVertical,
  FileText,
  Image as ImageIcon,
  X,
  Link as LinkIcon,
  Copy,
  Shield,
  Clock,
  Users,
  ChevronDown,
  Check,
} from "lucide-react";
import "./styles.css";
const API =
  (import.meta.env.VITE_API_URL || "http://localhost:8081/api").replace(/\/$/, "");

const req = async (path, opts = {}) => {
  const token = sessionStorage.getItem("token");

  const headers = {
    ...(opts.headers || {}),
  };

  if (token) {
    headers.Authorization = `Bearer ${token}`;
  }

  const response = await fetch(API + path, {
    ...opts,
    headers,
  });

  if (!response.ok) {
    let message = "Request failed";

    try {
      const data = await response.json();
      message = data.message || message;
    } catch {}

    throw new Error(message);
  }

  if (response.status === 204) {
    return null;
  }

  const contentType = response.headers.get("content-type") || "";

  if (contentType.includes("application/json")) {
    return response.json();
  }

  return response;
};


function PublicSharePage() {
  const token = window.location.pathname.split("/").filter(Boolean).pop();
  const [info, setInfo] = useState(null);
  const [password, setPassword] = useState("");
  const [loading, setLoading] = useState(true);
  const [downloading, setDownloading] = useState(false);
  const [error, setError] = useState("");

  useEffect(() => {
    if (!token) {
      setError("Invalid public link.");
      setLoading(false);
      return;
    }

    fetch(`${API}/public/${encodeURIComponent(token)}`)
      .then(async (response) => {
        const data = await response.json().catch(() => ({}));
        if (!response.ok) {
          throw new Error(data.message || "This public link is unavailable.");
        }
        setInfo(data);
      })
      .catch((err) => setError(err.message))
      .finally(() => setLoading(false));
  }, [token]);

  const download = async () => {
    setDownloading(true);
    setError("");

    try {
      const query = info?.protected
        ? `?password=${encodeURIComponent(password)}`
        : "";

      const response = await fetch(
        `${API}/public/${encodeURIComponent(token)}/download${query}`
      );

      if (!response.ok) {
        const data = await response.json().catch(() => ({}));
        throw new Error(
          data.message ||
          (response.status === 401
            ? "Incorrect password."
            : "Unable to download this file.")
        );
      }

      const blob = await response.blob();
      const url = URL.createObjectURL(blob);
      const anchor = document.createElement("a");
      anchor.href = url;
      anchor.download = info?.name || "download";
      document.body.appendChild(anchor);
      anchor.click();
      anchor.remove();
      URL.revokeObjectURL(url);
    } catch (err) {
      setError(err.message);
    } finally {
      setDownloading(false);
    }
  };

  if (loading) {
    return (
      <div className="public-page">
        <div className="public-card">
          <div className="loading-spinner" />
          <h1>Loading shared file...</h1>
        </div>
      </div>
    );
  }

  if (error && !info) {
    return (
      <div className="public-page">
        <div className="public-card">
          <div className="public-icon">!</div>
          <h1>Link unavailable</h1>
          <p>{error}</p>
        </div>
      </div>
    );
  }

  return (
    <div className="public-page">
      <div className="public-card">
        <div className="public-brand">
          <div className="logo">☁</div>
          <div>
            <b>CloudVault</b>
            <span>Secure file sharing</span>
          </div>
        </div>

        <div className="public-file-icon">
          <FileText size={30} />
        </div>

        <h1>{info.name}</h1>
        <p>
          {info.size >= 1024 * 1024
            ? `${(info.size / (1024 * 1024)).toFixed(1)} MB`
            : `${Math.max(1, Math.round(info.size / 1024))} KB`}
        </p>

        {info.protected && (
          <div className="public-password">
            <label>Protected link</label>
            <input
              type="password"
              placeholder="Enter password"
              value={password}
              onChange={(e) => setPassword(e.target.value)}
              onKeyDown={(e) => {
                if (e.key === "Enter") download();
              }}
            />
          </div>
        )}

        {error && <div className="error">{error}</div>}

        <button
          className="primary full"
          onClick={download}
          disabled={downloading || (info.protected && !password)}
        >
          {downloading ? "Downloading..." : "Download file"}
        </button>

        {info.expiresAt && (
          <small className="public-expiry">
            Expires {new Date(info.expiresAt).toLocaleString()}
          </small>
        )}
      </div>
    </div>
  );
}

/* =========================================================
   AUTH
========================================================= */

function Auth({ onLogin }) {
  const [mode, setMode] = useState("login");

  const [form, setForm] = useState({
    name: "",
    email: "",
    password: "",
  });

  const [error, setError] = useState("");

  const submit = async (event) => {
    event.preventDefault();
    setError("");

    try {
      const data = await req(`/auth/${mode}`, {
        method: "POST",
        headers: {
          "Content-Type": "application/json",
        },
        body: JSON.stringify(form),
      });

      sessionStorage.setItem("token", data.token);

      onLogin(data.user);
    } catch (error) {
      setError(error.message);
    }
  };

  return (
    <div className="auth">
      <div className="auth-card">
        <div className="brand">
          <div className="logo">☁</div>

          <div>
            <b>CloudVault</b>
            <small>Secure file storage</small>
          </div>
        </div>

        <h1>
          {mode === "login"
            ? "Welcome back"
            : "Create your account"}
        </h1>

        <p className="muted">
          {mode === "login"
            ? "Sign in to your cloud workspace."
            : "Start securely storing your files."}
        </p>

        <form onSubmit={submit}>
          {mode === "register" && (
            <input
              placeholder="Full name"
              value={form.name}
              onChange={(e) =>
                setForm({
                  ...form,
                  name: e.target.value,
                })
              }
              required
            />
          )}

          <input
            type="email"
            placeholder="Email"
            required
            value={form.email}
            onChange={(e) =>
              setForm({
                ...form,
                email: e.target.value,
              })
            }
          />

          <input
            type="password"
            placeholder="Password"
            required
            minLength={6}
            value={form.password}
            onChange={(e) =>
              setForm({
                ...form,
                password: e.target.value,
              })
            }
          />

          {error && <div className="error">{error}</div>}

          <button className="primary full">
            {mode === "login"
              ? "Sign in"
              : "Create account"}
          </button>
        </form>

        <button
          className="link"
          onClick={() =>
            setMode(
              mode === "login"
                ? "register"
                : "login"
            )
          }
        >
          {mode === "login"
            ? "New here? Create an account"
            : "Already have an account? Sign in"}
        </button>
      </div>
    </div>
  );
}

/* =========================================================
   SHARE MODAL
========================================================= */

function ShareModal({
  file,
  onClose,
  onNotice,
}) {
  const [email, setEmail] = useState("");
  const [role, setRole] = useState("VIEWER");

  const [shares, setShares] = useState([]);

  const [publicLinks, setPublicLinks] = useState([]);

  const [expiration, setExpiration] = useState("");

  const [password, setPassword] = useState("");

  const [loading, setLoading] = useState(false);

  const [creatingLink, setCreatingLink] =
    useState(false);

  const [activeTab, setActiveTab] =
    useState("people");

  const loadShares = async () => {
    try {
      const data = await req(
        `/shares/file/${file.id}`
      );

      setShares(data || []);
    } catch (error) {
      console.error(error);
    }
  };

  const loadPublicLinks = async () => {
    try {
      const data = await req(
        `/public-links/file/${file.id}`
      );

      setPublicLinks(data || []);
    } catch (error) {
      console.error(error);
    }
  };

  useEffect(() => {
    loadShares();
    loadPublicLinks();
  }, [file.id]);

  const shareWithUser = async (event) => {
    event.preventDefault();

    if (!email.trim()) {
      return;
    }

    setLoading(true);

    try {
      await req("/shares", {
        method: "POST",
        body: new URLSearchParams({
          fileId: file.id,
          email: email.trim(),
          role,
        }),
      });

      setEmail("");

      await loadShares();

      onNotice("File shared successfully.");
    } catch (error) {
      onNotice(error.message);
    } finally {
      setLoading(false);
    }
  };

  const removeShare = async (share) => {
    if (!window.confirm("Remove this person's access?")) {
      return;
    }

    try {
      await req(
        `/shares/${file.id}/${share.sharedWith.id}`,
        {
          method: "DELETE",
        }
      );

      await loadShares();

      onNotice("Access removed.");
    } catch (error) {
      onNotice(error.message);
    }
  };

  const createPublicLink = async () => {
    setCreatingLink(true);

    try {
      const body = new URLSearchParams();

      body.append("fileId", file.id);

      if (expiration) {
        body.append(
          "expiresInHours",
          expiration
        );
      }

      if (password.trim()) {
        body.append("password", password);
      }

      await req("/public-links", {
        method: "POST",
        body,
      });

      setPassword("");
      setExpiration("");

      await loadPublicLinks();

      onNotice("Public link created.");
    } catch (error) {
      onNotice(error.message);
    } finally {
      setCreatingLink(false);
    }
  };

  const deletePublicLink = async (link) => {
    if (!window.confirm("Revoke this public link?")) {
      return;
    }

    try {
      await req(`/public-links/${link.id}`, {
        method: "DELETE",
      });

      await loadPublicLinks();

      onNotice("Public link revoked.");
    } catch (error) {
      onNotice(error.message);
    }
  };

  const copyLink = async (link) => {
  const url = `${window.location.origin}/public/${link.token}`;

  try {
    await navigator.clipboard.writeText(url);
    onNotice("Public link copied.");
  } catch {
    onNotice("Unable to copy link.");
  }
};

  return (
    <div
      className="modal-overlay"
      onMouseDown={(e) => {
        if (e.target === e.currentTarget) {
          onClose();
        }
      }}
    >
      <div className="share-modal">
        <div className="modal-header">
          <div>
            <span className="modal-eyebrow">
              SHARING
            </span>

            <h2>Share file</h2>

            <p title={file.name}>
              {file.name}
            </p>
          </div>

          <button
            className="icon-button"
            onClick={onClose}
          >
            <X size={20} />
          </button>
        </div>

        <div className="share-tabs">
          <button
            className={
              activeTab === "people"
                ? "active"
                : ""
            }
            onClick={() =>
              setActiveTab("people")
            }
          >
            <Users size={17} />
            People
          </button>

          <button
            className={
              activeTab === "link"
                ? "active"
                : ""
            }
            onClick={() =>
              setActiveTab("link")
            }
          >
            <LinkIcon size={17} />
            Public link
          </button>
        </div>

        {activeTab === "people" && (
          <div className="share-section">
            <div className="section-heading">
              <div>
                <h3>People with access</h3>
                <span>
                  Manage who can access this file.
                </span>
              </div>
            </div>

            <div className="share-list">
              {shares.length === 0 ? (
                <div className="share-empty">
                  <Users size={25} />
                  <span>
                    No other users have access.
                  </span>
                </div>
              ) : (
                shares.map((share) => (
                  <div
                    className="share-person"
                    key={share.id}
                  >
                    <div className="person-avatar">
                      {share.sharedWith?.name
                        ?.charAt(0)
                        ?.toUpperCase() ||
                        share.sharedWith?.email
                          ?.charAt(0)
                          ?.toUpperCase() ||
                        "U"}
                    </div>

                    <div className="person-info">
                      <b>
                        {share.sharedWith?.name ||
                          share.sharedWith?.email}
                      </b>

                      <span>
                        {share.sharedWith?.email}
                      </span>
                    </div>

                    <span className="role-badge">
                      {share.role}
                    </span>

                    <button
                      className="remove-share"
                      onClick={() =>
                        removeShare(share)
                      }
                      title="Remove access"
                    >
                      <X size={16} />
                    </button>
                  </div>
                ))
              )}
            </div>

            <div className="add-person">
              <h3>Add people</h3>

              <form
                className="add-person-form"
                onSubmit={shareWithUser}
              >
                <input
                  type="email"
                  placeholder="Email address"
                  value={email}
                  onChange={(e) =>
                    setEmail(e.target.value)
                  }
                  required
                />

                <div className="role-select">
                  <select
                    value={role}
                    onChange={(e) =>
                      setRole(e.target.value)
                    }
                  >
                    <option value="VIEWER">
                      Viewer
                    </option>

                    <option value="EDITOR">
                      Editor
                    </option>
                  </select>

                  <ChevronDown size={15} />
                </div>

                <button
                  className="primary"
                  disabled={loading}
                >
                  {loading
                    ? "Sharing..."
                    : "Share"}
                </button>
              </form>
            </div>
          </div>
        )}

        {activeTab === "link" && (
          <div className="share-section">
            <div className="public-link-intro">
              <div className="public-link-icon">
                <LinkIcon size={22} />
              </div>

              <div>
                <h3>Anyone with the link</h3>

                <p>
                  Create a public link that can be
                  shared with anyone.
                </p>
              </div>
            </div>

            <div className="link-options">
              <label>
                <span>
                  <Clock size={16} />
                  Expiration
                </span>

                <select
                  value={expiration}
                  onChange={(e) =>
                    setExpiration(e.target.value)
                  }
                >
                  <option value="">
                    Never
                  </option>

                  <option value="1">
                    1 hour
                  </option>

                  <option value="24">
                    24 hours
                  </option>

                  <option value="72">
                    3 days
                  </option>

                  <option value="168">
                    7 days
                  </option>

                  <option value="720">
                    30 days
                  </option>
                </select>
              </label>

              <label>
                <span>
                  <Shield size={16} />
                  Password
                </span>

                <input
                  type="password"
                  placeholder="Optional password"
                  minLength={4}
                  value={password}
                  onChange={(e) =>
                    setPassword(e.target.value)
                  }
                />
              </label>
            </div>

            <button
              className="primary create-link-button"
              onClick={createPublicLink}
              disabled={creatingLink}
            >
              <LinkIcon size={17} />

              {creatingLink
                ? "Creating..."
                : "Create public link"}
            </button>

            <div className="existing-links">
              <div className="section-heading">
                <div>
                  <h3>Active public links</h3>
                </div>

                <span>
                  {publicLinks.length}
                </span>
              </div>

              {publicLinks.length === 0 ? (
                <div className="share-empty">
                  <LinkIcon size={25} />
                  <span>
                    No public links created yet.
                  </span>
                </div>
              ) : (
                publicLinks.map((link) => (
                  <div
                    className="public-link-card"
                    key={link.id}
                  >
                    <div className="public-link-card-icon">
                      <LinkIcon size={18} />
                    </div>

                    <div className="public-link-details">
                      <b>
                        Public access link
                      </b>

                      <span>
                        {link.expiresAt
                          ? `Expires ${new Date(
                              link.expiresAt
                            ).toLocaleString()}`
                          : "Never expires"}
                      </span>

                      {link.passwordHash && (
                        <small>
                          <Shield size={12} />
                          Password protected
                        </small>
                      )}
                    </div>

                    <button
                      className="copy-link"
                      onClick={() =>
                        copyLink(link)
                      }
                      title="Copy link"
                    >
                      <Copy size={16} />
                    </button>

                    <button
                      className="remove-link"
                      onClick={() =>
                        deletePublicLink(link)
                      }
                      title="Revoke link"
                    >
                      <Trash2 size={16} />
                    </button>
                  </div>
                ))
              )}
            </div>
          </div>
        )}

        <div className="modal-footer">
          <div>
            <Shield size={15} />
            Your files remain protected.
          </div>

          <button
            className="secondary"
            onClick={onClose}
          >
            Done
          </button>
        </div>
      </div>
    </div>
  );
}

/* =========================================================
   APP
========================================================= */

function App() {
  const [user, setUser] = useState(null);

  const [view, setView] = useState("drive");

  const [folder, setFolder] = useState(null);

  const [folders, setFolders] = useState([]);

  const [files, setFiles] = useState([]);

  const [query, setQuery] = useState("");

  const [loading, setLoading] = useState(false);

  const [grid, setGrid] = useState(true);

  const [notice, setNotice] = useState("");

  const [shareFile, setShareFile] =
    useState(null);

  useEffect(() => {
    if (!sessionStorage.getItem("token")) {
      return;
    }

    req("/auth/me")
      .then(setUser)
      .catch(() => {
        sessionStorage.removeItem("token");
      });
  }, []);

  const load = async () => {
    if (!user) return;

    setLoading(true);

    try {
      if (view === "trash") {
        setFiles(await req("/files/trash"));
      } else if (view === "starred") {
        setFiles(await req("/files/starred"));
      } else if (view === "shared") {
        const shared = await req("/shares");

        setFiles(
          shared.map((item) => ({
            ...item.file,
            sharedRole: item.role,
          }))
        );
      } else {
        const folderPath = folder
          ? `?folderId=${folder}`
          : "";

        const fileQuery = query
          ? `${folder ? "&" : "?"}q=${encodeURIComponent(
              query
            )}`
          : "";

        setFolders(
          await req(
            `/folders${folderPath}`
          )
        );

        setFiles(
          await req(
            `/files${folderPath}${fileQuery}`
          )
        );
      }
    } catch (error) {
      setNotice(error.message);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    load();
  }, [user, view, folder, query]);

  if (!user) {
    return <Auth onLogin={setUser} />;
  }

  const logout = () => {
    sessionStorage.removeItem("token");
    setUser(null);
  };

  const showNotice = (message) => {
    setNotice(message);

    setTimeout(() => {
      setNotice("");
    }, 3000);
  };

  const upload = async (event) => {
    const file = event.target.files?.[0];

    if (!file) return;

    const formData = new FormData();

    formData.append("file", file);

    if (folder) {
      formData.append("folderId", folder);
    }

    showNotice("Uploading...");

    try {
      await req("/files/upload", {
        method: "POST",
        body: formData,
      });

      showNotice("Upload complete.");

      await load();
    } catch (error) {
      showNotice(error.message);
    }

    event.target.value = "";
  };

  const createFolder = async () => {
    const name = window.prompt(
      "Folder name"
    );

    if (!name?.trim()) return;

    try {
      const body = new URLSearchParams({
        name: name.trim(),
      });

      if (folder) {
        body.append("parentId", folder);
      }

      await req("/folders", {
        method: "POST",
        body,
      });

      showNotice("Folder created.");

      await load();
    } catch (error) {
      showNotice(error.message);
    }
  };

  const download = async (file) => {
    try {
      const response = await req(
        `/files/${file.id}/download`
      );

      const blob = await response.blob();

      const url =
        URL.createObjectURL(blob);

      const anchor =
        document.createElement("a");

      anchor.href = url;
      anchor.download = file.name;

      document.body.appendChild(anchor);

      anchor.click();

      anchor.remove();

      URL.revokeObjectURL(url);
    } catch (error) {
      showNotice(error.message);
    }
  };

  const trash = async (file) => {
    try {
      await req(`/files/${file.id}`, {
        method: "DELETE",
      });

      showNotice("File moved to trash.");

      await load();
    } catch (error) {
      showNotice(error.message);
    }
  };

  const restore = async (file) => {
    try {
      await req(
        `/files/${file.id}/restore`,
        {
          method: "POST",
        }
      );

      showNotice("File restored.");

      await load();
    } catch (error) {
      showNotice(error.message);
    }
  };

  const star = async (file) => {
    try {
      const result = await req(
        `/files/${file.id}/star`,
        {
          method: "POST",
        }
      );

      showNotice(
        result.starred
          ? "Added to starred."
          : "Removed from starred."
      );

      await load();
    } catch (error) {
      showNotice(error.message);
    }
  };

  const openShare = (file) => {
    setShareFile(file);
  };

  const getViewTitle = () => {
    if (view === "drive") {
      return folder ? "Folder" : "All files";
    }

    if (view === "starred") {
      return "Starred files";
    }

    if (view === "shared") {
      return "Shared with you";
    }

    return "Trash";
  };

  const getViewEyebrow = () => {
    if (view === "drive") {
      return "MY DRIVE";
    }

    return view.toUpperCase();
  };

  return (
    <div className="app">
      {/* =================================================
          SIDEBAR
      ================================================= */}

      <aside>
        <div className="brand sidebrand">
          <div className="logo">☁</div>

          <div>
            <b>CloudVault</b>
            <small>Secure file storage</small>
          </div>
        </div>

        <button
          className="upload"
          onClick={() =>
            document
              .getElementById("upload")
              .click()
          }
        >
          <Upload size={18} />
          Upload file
        </button>

        <input
          id="upload"
          type="file"
          hidden
          onChange={upload}
        />

        <nav>
          <button
            className={
              view === "drive"
                ? "active"
                : ""
            }
            onClick={() => {
              setView("drive");
              setFolder(null);
              setQuery("");
            }}
          >
            <Home />
            My Drive
          </button>

          <button
            className={
              view === "starred"
                ? "active"
                : ""
            }
            onClick={() => {
              setView("starred");
              setFolder(null);
            }}
          >
            <Star />
            Starred
          </button>

          <button
            className={
              view === "shared"
                ? "active"
                : ""
            }
            onClick={() => {
              setView("shared");
              setFolder(null);
            }}
          >
            <Share2 />
            Shared with me
          </button>

          <button
            className={
              view === "trash"
                ? "active"
                : ""
            }
            onClick={() => {
              setView("trash");
              setFolder(null);
            }}
          >
            <Trash2 />
            Trash
          </button>
        </nav>

        <div className="storage">
          <div className="storage-title">
            <span>
              <Shield size={14} />
              Storage
            </span>
          </div>

          <div className="meter">
            <span style={{ width: "18%" }} />
          </div>

          <span>
            18% of local storage used
          </span>
        </div>

        <div className="sidebar-user">
          <div className="user-avatar">
            {user.name
              ?.charAt(0)
              ?.toUpperCase() || "U"}
          </div>

          <div>
            <b>{user.name}</b>
            <span>{user.email}</span>
          </div>
        </div>

        <button
          className="logout"
          onClick={logout}
        >
          <LogOut />
          Sign out
        </button>
      </aside>

      {/* =================================================
          MAIN
      ================================================= */}

      <main>
        <header>
          <button className="menu-button">
            <MoreVertical size={20} />
          </button>

          <div className="search">
            <Search size={18} />

            <input
              placeholder="Search your files..."
              value={query}
              onChange={(event) => {
                setView("drive");
                setFolder(null);
                setQuery(event.target.value);
              }}
            />
          </div>

          <div className="header-user">
            <span>Secure</span>

            <div className="avatar">
              {user.name
                ?.charAt(0)
                ?.toUpperCase() || "U"}
            </div>
          </div>
        </header>

        <section className="content">
          <div className="title-row">
            <div>
              <div className="eyebrow">
                {getViewEyebrow()}
              </div>

              <h2>
                {getViewTitle()}
              </h2>

              {view === "drive" && (
                <p className="page-description">
                  Manage and organize your
                  cloud files.
                </p>
              )}
            </div>

            <div className="actions">
              {view === "drive" && (
                <>
                  <button
                    onClick={createFolder}
                    className="secondary"
                  >
                    <Plus size={17} />
                    New folder
                  </button>

                  <button
                    className="primary"
                    onClick={() =>
                      document
                        .getElementById(
                          "upload"
                        )
                        .click()
                    }
                  >
                    <Upload size={17} />
                    Upload
                  </button>
                </>
              )}

              <button
                className="view-toggle"
                onClick={() =>
                  setGrid(!grid)
                }
                title="Change view"
              >
                {grid ? (
                  <List size={18} />
                ) : (
                  <Grid2X2 size={18} />
                )}
              </button>
            </div>
          </div>

          {folder && (
            <div className="breadcrumb">
              <button
                onClick={() =>
                  setFolder(null)
                }
              >
                My Drive
              </button>

              <span>/</span>

              <span>Current folder</span>
            </div>
          )}

          {notice && (
            <div className="notice">
              <Check size={17} />
              {notice}
            </div>
          )}

          {view === "drive" && (
            <>
              <div className="section-title-row">
                <h3>Folders</h3>

                <span>
                  {folders.length}{" "}
                  {folders.length === 1
                    ? "folder"
                    : "folders"}
                </span>
              </div>

              {folders.length === 0 ? (
                <div className="folder-empty">
                  <Folder size={24} />
                  <span>
                    No folders yet
                  </span>

                  <button
                    onClick={createFolder}
                  >
                    Create folder
                  </button>
                </div>
              ) : (
                <div className="folder-grid">
                  {folders.map((item) => (
                    <button
                      className="folder-card"
                      key={item.id}
                      onDoubleClick={() =>
                        setFolder(item.id)
                      }
                    >
                      <Folder />

                      <span title={item.name}>
                        {item.name}
                      </span>

                      <MoreVertical
                        size={16}
                      />
                    </button>
                  ))}
                </div>
              )}
            </>
          )}

          {view === "drive" && (
            <div className="section-title-row files-heading">
              <h3>Files</h3>

              <span>
                {files.length}{" "}
                {files.length === 1
                  ? "file"
                  : "files"}
              </span>
            </div>
          )}

          {loading ? (
            <div className="empty">
              <div className="loading-spinner" />
              <b>Loading files...</b>
            </div>
          ) : files.length === 0 ? (
            <div className="empty">
              <FileText size={42} />

              <b>No files here</b>

              <span>
                Upload a file to get started.
              </span>

              {view === "drive" && (
                <button
                  className="empty-upload"
                  onClick={() =>
                    document
                      .getElementById(
                        "upload"
                      )
                      .click()
                  }
                >
                  <Upload size={16} />
                  Upload a file
                </button>
              )}
            </div>
          ) : (
            <div
              className={
                grid
                  ? "file-grid"
                  : "file-list"
              }
            >
              {files.map((file) => (
                <article
                  className="file-card"
                  key={file.id}
                >
                  <div className="file-icon">
                    {file.contentType?.startsWith(
                      "image/"
                    ) ? (
                      <ImageIcon />
                    ) : (
                      <FileText />
                    )}
                  </div>

                  <div className="file-info">
                    <b
                      title={file.name}
                    >
                      {file.name}
                    </b>

                    <span>
                      {Math.max(
                        1,
                        Math.round(
                          (file.size || 0) /
                            1024
                        )
                      )}{" "}
                      KB
                    </span>
                  </div>

                  <div className="card-actions">
                    {view === "trash" ? (
                      <button
                        title="Restore"
                        onClick={() =>
                          restore(file)
                        }
                      >
                        ↩
                      </button>
                    ) : (
                      <>
                        <button
                          title="Star"
                          onClick={() =>
                            star(file)
                          }
                          className={
                            file.starred
                              ? "starred"
                              : ""
                          }
                        >
                          <Star size={16} />
                        </button>

                        <button
                          title="Share"
                          onClick={() =>
                            openShare(file)
                          }
                        >
                          <Share2
                            size={16}
                          />
                        </button>

                        <button
                          title="Download"
                          onClick={() =>
                            download(file)
                          }
                        >
                          <Download
                            size={16}
                          />
                        </button>

                        <button
                          title="Trash"
                          onClick={() =>
                            trash(file)
                          }
                        >
                          <Trash2 size={16} />
                        </button>
                      </>
                    )}
                  </div>
                </article>
              ))}
            </div>
          )}
        </section>
      </main>

      {/* =================================================
          SHARE MODAL
      ================================================= */}

      {shareFile && (
        <ShareModal
          file={shareFile}
          onClose={() =>
            setShareFile(null)
          }
          onNotice={showNotice}
        />
      )}
    </div>
  );
}

/* =========================================================
   ROOT
========================================================= */

const isPublicShare =
  window.location.pathname.startsWith("/public/");

createRoot(document.getElementById("root")).render(
  isPublicShare ? <PublicSharePage /> : <App />
);
