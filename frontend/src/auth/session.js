const STORAGE_KEY = 'travelmind.session';
let memorySession = null;

function storage() {
  try {
    return typeof window === 'undefined' ? null : window.sessionStorage;
  } catch {
    return null;
  }
}

function read() {
  try {
    const persisted = storage()?.getItem(STORAGE_KEY);
    return persisted ? JSON.parse(persisted) : memorySession;
  } catch {
    return memorySession;
  }
}

export const authSession = {
  get() {
    return read();
  },
  save(session) {
    memorySession = session;
    storage()?.setItem(STORAGE_KEY, JSON.stringify(session));
    return session;
  },
  updateUser(user) {
    const session = read();
    return session ? this.save({ ...session, user }) : null;
  },
  clear() {
    memorySession = null;
    storage()?.removeItem(STORAGE_KEY);
  },
  token() {
    return read()?.tokenValue || '';
  },
  user() {
    return read()?.user || null;
  },
  hasRole(role) {
    return read()?.user?.roles?.includes(role) || false;
  },
  isLoggedIn() {
    return Boolean(read()?.tokenValue);
  },
};
