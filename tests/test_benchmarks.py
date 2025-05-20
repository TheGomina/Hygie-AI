import pytest
from backend.bmp_service.interactions_ref import _load_from_csv, _load_from_pdf

@pytest.mark.benchmark(group="csv_load")
def test_load_csv_perf(benchmark, tmp_path):
    # Generate large CSV (~10k lines)
    data = "\n".join([f"DRUG{i};{i%5};;" for i in range(10000)])
    fp = tmp_path / "cia.csv"
    fp.write_text(data, encoding="utf-8")
    # Run benchmark
    benchmark.pedantic(_load_from_csv, args=(fp,), iterations=1, rounds=5)
    # Assert mean latency
    assert benchmark.stats['mean'] < 0.05

@pytest.mark.benchmark(group="pdf_parse")
def test_load_pdf_perf(benchmark, tmp_path, monkeypatch):
    # Stub extract_text to return many lines
    text = "\n".join([f"DRUG{i} / DRUG{i+1}" for i in range(10000)])
    fp = tmp_path / "foo.pdf"
    fp.write_text("", encoding="utf-8")
    monkeypatch.setattr("backend.bmp_service.interactions_ref.extract_text", lambda _: text)
    # Run benchmark
    benchmark.pedantic(_load_from_pdf, args=(fp,), iterations=1, rounds=5)
    # Assert mean latency
    assert benchmark.stats['mean'] < 0.02
