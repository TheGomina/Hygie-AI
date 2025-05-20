import pytest

from backend.bmp_service.renal import calculate_clcr, adjust_for_renal


def test_calculate_clcr_male():
    # Typical values
    clcr = calculate_clcr(creatinine=1.0, age=40, weight=70, sex="M")
    assert clcr > 0


def test_calculate_clcr_female():
    clcr = calculate_clcr(1.0, 40, 70, "F")
    # Female factor reduces value
    assert clcr < calculate_clcr(1.0, 40, 70, "M")


def test_calculate_clcr_invalid():
    with pytest.raises(AssertionError):
        calculate_clcr(0, 40, 70, "M")
    with pytest.raises(AssertionError):
        calculate_clcr(1.0, -1, 70, "M")
    with pytest.raises(AssertionError):
        calculate_clcr(1.0, 40, 0, "M")


def test_adjust_for_renal_clinical():
    # ClCr <30
    assert adjust_for_renal(10, 80, 60, "M") == "Contre-indication rénale"
    # ClCr between 30 and 60
    clcr = calculate_clcr(1.0, 50, 60, "M")
    if clcr < 60 and clcr >= 30:
        assert adjust_for_renal(1.0, 50, 60, "M") == "Réduction 50% posologique"
    # ClCr >= 60
    assert adjust_for_renal(0.5, 20, 80, "M") == "Posologie normale"
