FROM prom/prometheus:latest
COPY prometheus-render.yml /etc/prometheus/prometheus.yml
