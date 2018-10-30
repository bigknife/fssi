package fssi

import fssi.interpreter.StackConsoleMain

package object tool {
  type Effect = StackConsoleMain.Effect
  type Program[A] = StackConsoleMain.Program[A]
}
