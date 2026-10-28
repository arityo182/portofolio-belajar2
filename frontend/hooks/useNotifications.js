"use client";

import { useEffect, useRef, useState } from "react";
import { Client } from "@stomp/stompjs";
import { useQueryClient } from "@tanstack/react-query";
import { useAuthStore } from "@/stores/authStore";
import { refreshAccessToken } from "@/services/api";
import { isTokenExpiring } from "@/lib/jwt";
import { loanKeys, notificationKeys, paymentKeys } from "@/lib/queryKeys";

const TOAST_DURATION_MS = 7000;

function buildBrokerUrl() {
  const configured = process.env.NEXT_PUBLIC_WS_URL;
  const fallback =
    typeof window !== "undefined"
      ? `${window.location.protocol === "https:" ? "wss" : "ws"}://${window.location.host}/ws`
      : null;
  const base = configured || fallback;
  if (!base) {
    return null;
  }

  try {
    return new URL(base).toString();
  } catch {
    return null;
  }
}

async function ensureFreshToken() {
  const { token, refreshToken } = useAuthStore.getState();
  if (!token) {
    return null;
  }
  if (!isTokenExpiring(token, 90)) {
    return token;
  }
  if (!refreshToken) {
    return token;
  }

  try {
    return await refreshAccessToken();
  } catch {
    useAuthStore.getState().logout();
    if (typeof window !== "undefined" && !window.location.pathname.startsWith("/login")) {
      window.location.assign("/login?expired=1");
    }
    return null;
  }
}

export function useNotifications() {
  const queryClient = useQueryClient();
  const userId = useAuthStore((state) => state.user?.userId);
  const authenticated = useAuthStore((state) => state.isAuthenticated);
  const [connected, setConnected] = useState(false);
  const [lastNotification, setLastNotification] = useState(null);
  const toastTimer = useRef(null);

  useEffect(() => {
    if (!authenticated || !userId) {
      setConnected(false);
      return undefined;
    }

    let active = true;
    const client = new Client({
      webSocketFactory: () => {
        const brokerUrl = buildBrokerUrl();
        if (!brokerUrl) {
          throw new Error("URL notifikasi tidak tersedia");
        }
        return new WebSocket(brokerUrl);
      },
      reconnectDelay: 5000,
      heartbeatIncoming: 0,
      heartbeatOutgoing: 10000,
      debug: () => {},
      beforeConnect: async () => {
        const token = await ensureFreshToken();
        if (!token) {
          throw new Error("Sesi notifikasi telah berakhir");
        }
        client.connectHeaders = { Authorization: `Bearer ${token}` };
      },
      onConnect: () => {
        if (!active) {
          return;
        }
        setConnected(true);
        client.subscribe(`/topic/notifications/${userId}`, (frame) => {
          let notification = null;
          try {
            notification = JSON.parse(frame.body);
          } catch {
            notification = null;
          }

          queryClient.invalidateQueries({ queryKey: loanKeys.all });
          queryClient.invalidateQueries({ queryKey: paymentKeys.all });
          // Refresh the list and the navbar unread badge as soon as a realtime
          // notification arrives.
          queryClient.invalidateQueries({ queryKey: notificationKeys.all });

          if (notification) {
            setLastNotification(notification);
            if (toastTimer.current) {
              window.clearTimeout(toastTimer.current);
            }
            toastTimer.current = window.setTimeout(() => {
              setLastNotification(null);
            }, TOAST_DURATION_MS);
          }
        });
      },
      onDisconnect: () => setConnected(false),
      onWebSocketClose: () => setConnected(false),
      onStompError: () => setConnected(false),
    });

    ensureFreshToken()
      .then((token) => {
        if (active && token) {
          client.activate();
        }
      })
      .catch(() => {});

    return () => {
      active = false;
      setConnected(false);
      if (toastTimer.current) {
        window.clearTimeout(toastTimer.current);
      }
      client.deactivate();
    };
  }, [authenticated, queryClient, userId]);

  return {
    connected,
    lastNotification,
    dismissNotification: () => setLastNotification(null),
  };
}
