# EMERGENCE — Scientific Assumptions & Limitations

1. **Bounded Perception:** Agents only observe cells and agents within their `perceptionRadius`, never the entire world state.
2. **Simplified Traits:** Behavioural traits (`aggression`, `cooperation`, `sociability`, `exploration`, `riskTolerance`, `trustPropensity`) are numeric parameters in `[0.0, 1.0]`, not psychological models.
3. **Simplified Resource Regeneration:** Cell resources regenerate linearly up to `maxResource` using `R(t+1) = min(Rmax, R(t) + G(t) - C(t))`.
4. **Computational Evolution:** Inheritance and mutation operate on numeric trait vectors with bounded uniform perturbations.
5. **Non-Predictive Scope:** Emergence is a controlled computational laboratory for studying artificial rules and emergent patterns; it does not predict real-world human societies, ecosystems, or epidemics.
