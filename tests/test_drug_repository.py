import pytest
from backend.bmp_service.drug_repository import _load_dispense_surveillance_rules

def test_load_dispense_surveillance_rules_keys():
    mapping = _load_dispense_surveillance_rules()
    assert isinstance(mapping, dict)
    assert 'ACETATE DE CHLORMADINONE' in mapping

def test_rules_structure():
    mapping = _load_dispense_surveillance_rules()
    rules = mapping['ACETATE DE CHLORMADINONE']
    assert 'prescriptions' in rules
    assert 'surveillances' in rules
    assert isinstance(rules['prescriptions'], str)
    assert isinstance(rules['surveillances'], str)
    # Surveillance rules should start with 'Pour'
    assert rules['surveillances'].startswith('Pour')
