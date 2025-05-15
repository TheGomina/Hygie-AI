# Stub for prometheus_client to satisfy imports in backend/auth_service/app.py
# Metrics are no-ops for testing.

CONTENT_TYPE_LATEST = "text/plain; version=0.0.4; charset=utf-8"


def generate_latest():
    return b""


class Counter:
    def __init__(self, *args, **kwargs):
        pass

    def labels(self, *args, **kwargs):
        return self

    def inc(self):
        pass


class Summary:
    def __init__(self, *args, **kwargs):
        pass

    def time(self):
        def decorator(func):
            return func

        return decorator
