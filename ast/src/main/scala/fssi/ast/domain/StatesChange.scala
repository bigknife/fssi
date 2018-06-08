package fssi.ast.domain

import fssi.contract.States

case class StatesChange(
    previous: States,
    current: States
)
