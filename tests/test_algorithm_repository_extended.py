import pytest
import yaml
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
    AlgorithmStep,
    PharmaAlgorithm,
)

@ pytest.fixture(autouse=True)
def fake_resources(monkeypatch):
    # Fake Path.read_text for YAML resources
    original_read_text = Path.read_text
    def fake_read_text(self, encoding='utf-8'):
        name = self.name
        if name == 'algorithm_template.yaml':
            return 'steps:\n  - id: s1\n    title: t1\n'
        elif name == 'classification_mapping.yaml':
            return yaml.dump({'c1': {'s': {'k': 'v'}}})
        elif name == 'algorithms_examples.yaml':
            return yaml.dump([{'name': 'algoX', 'steps': [{'id': 's1', 'content': 'cont1'}]}])
        return original_read_text(self, encoding)
    monkeypatch.setattr(Path, 'read_text', fake_read_text)
    # Ensure existence
    original_exists = Path.exists
    def fake_exists(self):
        if self.name in ('classification_mapping.yaml', 'algorithms_examples.yaml'):
            return True
        return original_exists(self)
    monkeypatch.setattr(Path, 'exists', fake_exists)
    yield


def test_load_and_create_algorithm():
    steps = load_algorithm_template()
    assert isinstance(steps, list) and steps
    assert steps[0].id == 's1' and steps[0].title == 't1'
    algo = create_algorithm('nameX')
    assert isinstance(algo, PharmaAlgorithm)
    assert algo.name == 'nameX'
    assert [step.id for step in algo.steps] == ['s1']


def test_load_classification_mapping():
    mapping = load_classification_mapping()
    assert mapping['c1']['s']['k'] == 'v'


def test_load_example_algorithms():
    algos = load_example_algorithms()
    assert len(algos) == 1
    a = algos[0]
    assert a.name == 'algoX'
    assert a.steps[0].id == 's1'
    assert a.steps[0].content == 'cont1'


def test_extract_elements_appreciation_good():
    text = 'k1: v1; k2: v2; ;'
    elems = extract_elements_appreciation(text)
    assert elems == {'k1': 'v1', 'k2': 'v2'}


def test_extract_elements_appreciation_errors():
    with pytest.raises(AssertionError):
        extract_elements_appreciation(123)
    with pytest.raises(AssertionError):
        extract_elements_appreciation('')


def test_elements_to_logic_and_errors():
    elements = {'a': 'b', 'c': 'd'}
    logic = elements_to_logic(elements)
    assert 'a -> b' in logic and 'AND' in logic
    with pytest.raises(AssertionError):
        elements_to_logic('not a dict')
    with pytest.raises(AssertionError):
        elements_to_logic({})


def test_logic_to_code():
    code = logic_to_code('a -> b AND c -> d')
    assert code.startswith('lambda patient: ')
    assert 'and' in code


def test_generate_algorithms_for_atc(monkeypatch):
    # Stub template
    monkeypatch.setattr(
        'backend.bmp_service.algorithm_repository.load_algorithm_template',
        lambda: [
            AlgorithmStep('situation', ''),
            AlgorithmStep('logical_rule', ''),
            AlgorithmStep('code_rule', ''),
            AlgorithmStep('intervention', ''),
            AlgorithmStep('references', ''),
            AlgorithmStep('bdc_reference', ''),
        ],
    )
    # Stub substances_from_atc
    monkeypatch.setattr(
        'backend.bmp_service.algorithm_repository.substances_from_atc',
        lambda code: ['sub1', 'sub2'] if code == 'X' else [],
    )
    algos = generate_algorithms_for_atc(['X', 'Y'])
    assert len(algos) == 1
    algo = algos[0]
    assert algo.name == 'AP_X'
    contents = {step.id: step.content for step in algo.steps}
    assert contents['situation'] == 'Patient sous classe ATC X'
    assert 'sub1' in contents['logical_rule']
    assert contents['code_rule'].startswith('lambda patient, meds: any')
    assert contents['references'] == 'ANAP'
    assert contents['bdc_reference'] == 'X'
