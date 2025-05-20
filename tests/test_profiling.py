import pytest
from pyinstrument import Profiler
from backend.bmp_service.interactions_ref import _load_from_csv, _load_from_pdf

@pytest.mark.skip(reason="Profiling tests, not for CI")
def test_profile_load_csv(tmp_path):
    # Generate large CSV (~10k lines)
    data = "\n".join([f"DRUG{i};{i%5};;" for i in range(10000)])
    fp = tmp_path / "cia.csv"
    fp.write_text(data, encoding="utf-8")

    profiler = Profiler()
    profiler.start()
    _load_from_csv(fp)
    profiler.stop()

    # Output profiling report to console
    print(profiler.output_text(unicode=True, color=False))

@pytest.mark.skip(reason="Profiling tests, not for CI")
def test_profile_load_pdf(tmp_path, monkeypatch):
    # Generate stubbed PDF text (~10k lines)
    text = "\n".join([f"DRUG{i} / DRUG{i+1}" for i in range(10000)])
    fp = tmp_path / "foo.pdf"
    fp.write_text("", encoding="utf-8")
    monkeypatch.setattr("backend.bmp_service.interactions_ref.extract_text", lambda _: text)

    profiler = Profiler()
    profiler.start()
    _load_from_pdf(fp)
    profiler.stop()

    # Output profiling report to console
    print(profiler.output_text(unicode=True, color=False))
