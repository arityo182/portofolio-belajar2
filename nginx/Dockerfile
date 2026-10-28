FROM nginx:1.27-alpine

RUN apk add --no-cache openssl curl

COPY nginx/nginx.conf /etc/nginx/nginx.conf
COPY nginx/conf.d/koperasi.conf /etc/nginx/conf.d/koperasi.conf
COPY nginx/proxy_params_koperasi.conf /etc/nginx/proxy_params_koperasi.conf
COPY nginx/proxy_params_stream.conf /etc/nginx/proxy_params_stream.conf
RUN rm -f /etc/nginx/conf.d/default.conf
COPY nginx/docker-entrypoint.d/10-generate-self-signed-cert.sh /docker-entrypoint.d/10-generate-self-signed-cert.sh
RUN chmod +x /docker-entrypoint.d/10-generate-self-signed-cert.sh

EXPOSE 80 443

HEALTHCHECK --interval=30s --timeout=5s --start-period=20s --retries=3 \
    CMD curl -fsSk https://localhost/healthz || exit 1
