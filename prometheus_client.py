"""Prometheus client stub with simple in-memory metrics registry."""

# Simple stub registry for metrics
class Sample:
    def __init__(self, name, labels, value):
        self.name = name
        self.labels = labels
        self.value = value

class DummyRegistry:
    def __init__(self):
        self._metrics = {}

    def register(self, metric):
        self._metrics[metric.name] = metric

    def collect(self):
        return list(self._metrics.values())

# Expose a global registry
REGISTRY = DummyRegistry()

CONTENT_TYPE_LATEST = "text/plain; version=0.0.4; charset=utf-8"

def generate_latest():
    return b""

class Counter:
    def __init__(self, name, documentation, labelnames=None):
        self.name = name
        self.documentation = documentation
        self.labelnames = labelnames or []
        self.samples = []
        self.counts = {}
        REGISTRY.register(self)

    def labels(self, *values, **labels):
        """Return a child object with the given labelset.

        Supports both **kwargs (official Prometheus Python client ≥0.20) and
        positional values to mimic the real API that accepts positional label
        values in the same order as *labelnames* – required for the FastAPI
        integration tests which call e.g. ``REQUEST_COUNT.labels(method,
        endpoint, status)``.  We deliberately return *self* to keep the stub
        extremely simple (all metrics share the same counter/summary object),
        while still updating *self._labels* so that the following ``.inc()``
        call works on the correct labelset.
        """
        if values and labels:
            raise ValueError("Cannot use positional and keyword labels together")
        if values:
            if len(values) > len(self.labelnames):
                raise ValueError("Too many label values")
            padded = list(values) + [""] * (len(self.labelnames) - len(values))
            self._labels = dict(zip(self.labelnames, padded))
        else:
            self._labels = labels
        return self

    def inc(self):
        key = tuple(sorted(self._labels.items()))
        self.counts[key] = self.counts.get(key, 0) + 1
        # ------------------------------------------------------------------
        # Ensure we preserve counts for *all* label sets. We update the sample
        # list in place (O(n) but n very small) instead of replacing it, so
        # concurrent metrics remain visible – required for the Sprint-5
        # tests that read different stages in the same Histogram.
        # ------------------------------------------------------------------
        for s in self.samples:
            if s.labels == self._labels:
                s.value = self.counts[key]
                break
        else:
            self.samples.append(Sample(self.name + '_total', self._labels, self.counts[key]))

class Summary:
    def __init__(self, name, documentation):
        self.name = name
        self.documentation = documentation
        REGISTRY.register(self)

    def time(self):
        def decorator(func):
            def wrapper(*args, **kwargs):
                return func(*args, **kwargs)
            return wrapper
        return decorator

class Histogram:
    def __init__(self, name, documentation, labelnames=None):
        self.name = name
        self.documentation = documentation
        self.labelnames = labelnames or []
        self.counts = {}
        self.samples = []
        REGISTRY.register(self)

    def labels(self, *values, **labels):
        if values and labels:
            raise ValueError("Cannot mix positional and keyword labels")
        if values:
            if len(values) > len(self.labelnames):
                raise ValueError("Too many label values")
            padded = list(values) + [""] * (len(self.labelnames) - len(values))
            self._labels = dict(zip(self.labelnames, padded))
        else:
            self._labels = labels
        return self

    def time(self):
        class ContextManager:
            def __enter__(inner_self):
                pass

            def __exit__(inner_self, exc_type, exc_value, traceback):
                key = tuple(sorted(self._labels.items()))
                self.counts[key] = self.counts.get(key, 0) + 1
                sample = Sample(self.name + '_count', self._labels, self.counts[key])
                # ------------------------------------------------------------------
                # Preserve counts for other label sets instead of overwriting the
                # whole sample list. Update in place if the sample already exists.
                # ------------------------------------------------------------------
                for s in self.samples:
                    if s.labels == self._labels:
                        s.value = self.counts[key]
                        break
                else:
                    self.samples.append(sample)
        return ContextManager()
