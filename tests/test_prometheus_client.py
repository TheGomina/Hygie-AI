import pytest
from prometheus_client import generate_latest, Counter, Summary, Histogram


def test_generate_latest_returns_bytes():
    result = generate_latest()
    assert isinstance(result, (bytes, bytearray))
    assert result == b""


def test_counter_labels_and_inc():
    c = Counter("test", "desc")
    returned = c.labels(stage="s")
    assert returned is c
    # inc should not raise
    c.inc()


def test_summary_time_decorator():
    s = Summary("test", "desc")
    decorator = s.time()
    # Decorate a function and ensure it returns correct value
    def func(x, y=1):
        return x + y
    decorated = decorator(func)
    assert callable(decorated)
    assert decorated(2) == 3


def test_histogram_labels_and_time():
    h = Histogram("test", "desc", ["label"])
    returned = h.labels(label="val")
    assert returned is h
    cm = h.time()
    # Context manager should have enter and exit
    assert hasattr(cm, "__enter__") and hasattr(cm, "__exit__")
    with cm:
        pass
