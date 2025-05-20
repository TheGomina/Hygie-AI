import pytest
from backend.bmp_service.renal import calculate_clcr, adjust_for_renal


def test_calculate_clcr_male():
    # (140-40)*70/(72*1) ≈ 97.22
    clcr = calculate_clcr(creatinine=1.0, age=40, weight=70, sex='M')
    assert pytest.approx((140-40)*70/(72*1), rel=1e-2) == clcr


def test_calculate_clcr_female():
    # (140-40)*60/(72*1)*0.85 ≈ 70.83
    clcr = calculate_clcr(creatinine=1.0, age=40, weight=60, sex='F')
    assert pytest.approx((140-40)*60/(72*1)*0.85, rel=1e-2) == clcr


def test_adjust_for_renal():
    assert adjust_for_renal(1.0, 40, 70, 'M') == 'Posologie normale'
    # clcr < 30
    assert adjust_for_renal(3.0, 70, 60, 'F') == 'Contre-indication rénale'
    # clcr between 30 and 60
    assert adjust_for_renal(1.5, 65, 65, 'F') == 'Réduction 50% posologique'


def test_calculate_clcr_invalid():
    with pytest.raises(AssertionError):
        calculate_clcr(0, 40, 70, 'M')
