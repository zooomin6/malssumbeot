import { API_BASE_URL } from "@/constants/api";
import * as SecureStore from "expo-secure-store";
import { createContext, ReactNode, useContext, useEffect, useState } from "react";

const TOKEN_KEY = "accessToken";

type AuthContextValue = {
  token: string | null;
  isLoading: boolean;
  loginWithDevToken: () => Promise<void>;
  logout: () => Promise<void>;
};

const AuthContext = createContext<AuthContextValue | undefined>(undefined);

/** 로그인 상태(토큰)를 앱 전체에서 공유한다. 실제 소셜 로그인 전까지는 dev-token만 지원. */
export function AuthProvider({ children }: { children: ReactNode }) {
  const [token, setToken] = useState<string | null>(null);
  const [isLoading, setIsLoading] = useState(true);

  useEffect(() => {
    SecureStore.getItemAsync(TOKEN_KEY).then((stored) => {
      setToken(stored);
      setIsLoading(false);
    });
  }, []);

  const loginWithDevToken = async () => {
    const res = await fetch(`${API_BASE_URL}/api/auth/dev-token`, { method: "POST" });
    const data = await res.json();
    await SecureStore.setItemAsync(TOKEN_KEY, data.accessToken);
    setToken(data.accessToken);
  };

  const logout = async () => {
    await SecureStore.deleteItemAsync(TOKEN_KEY);
    setToken(null);
  };

  return (
    <AuthContext.Provider value={{ token, isLoading, loginWithDevToken, logout }}>
      {children}
    </AuthContext.Provider>
  );
}

export function useAuth() {
  const ctx = useContext(AuthContext);
  if (!ctx) {
    throw new Error("useAuth는 AuthProvider 안에서만 쓸 수 있다");
  }
  return ctx;
}
