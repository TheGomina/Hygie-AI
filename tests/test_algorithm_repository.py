import pytest
from pathlib import Path

from backend.bmp_service.algorithm_repository import (
    load_algorithm_template,
    create_algorithm,
    load_classification_mapping,
    load_example_algorithms,
    extract_elements_appreciation,
    elements_to_logic,
    logic_to_code,
    generate_algorithms_for_atc,
)
from backend.bmp_service.drug_repository import supported_atc_codes


def test_load_algorithm_template():
    steps = load_algorithm_template()
    ids = [s.id for s in steps]
    assert ids == [
        'situation', 'logical_rule', 'code_rule', 'intervention',
        'references', 'bdc_reference', 'template'
    ]
    assert all(isinstance(s.title, str) and s.title for s in steps)


def test_create_algorithm():
    algo = create_algorithm('TEST')
    assert algo.name == 'TEST'
    assert len(algo.steps) == len(load_algorithm_template())
    assert all(step.content == '' for step in algo.steps)


def test_load_classification_mapping():
    mapping = load_classification_mapping()
    for cat in ['gravite', 'plp', 'ip']:
        assert cat in mapping
        for country in ['belgique', 'france', 'suisse']:
            assert country in mapping[cat]


def test_extract_and_convert_elements():
    text = 'age:>65; hemoglobine:<12; condition:stable'
    elems = extract_elements_appreciation(text)
    assert elems == {'age': '>65', 'hemoglobine': '<12', 'condition': 'stable'}
    logic = elements_to_logic(elems)
    assert 'age -> >65' in logic and 'AND' in logic
    code = logic_to_code(logic)
    assert code.startswith('lambda')


def test_load_example_algorithms():
    examples = load_example_algorithms()
    assert isinstance(examples, list) and examples
    assert any(e.name.startswith('AP00') for e in examples)


def test_supported_atc_and_generate():
    codes = supported_atc_codes()
    assert isinstance(codes, list) and codes
    # test generation for one ATC code
    test_code = codes[0]
    algos = generate_algorithms_for_atc([test_code])
    if algos:
        algo = algos[0]
        assert algo.name == f'AP_{test_code}'
        # code_rule step exists
        code_steps = [s for s in algo.steps if s.id == 'code_rule']
        assert code_steps and code_steps[0].content.startswith('lambda')
