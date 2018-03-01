// ALU Unit Tester
package adept.alu

import chisel3.iotesters
import chisel3.iotesters.{ChiselFlatSpec, Driver, PeekPokeTester}

import adept.config.AdeptConfig

/*
 * ALU Unit Tester
 *
 * The unit tester will run through all ALU pertinent instructions to check for
 * their correct functionality. Each test should note what each input is doing,
 * and why it's important, followed by its expected output. In the case of an
 * edge case test, the author should provide a detailed explanation of why their
 * set of inputs fails to achieve the correct result, and why their fix works.
 * If need be, please quote the ISA specification.
 */
class ALUUnitTester(c: ALU) extends PeekPokeTester(c) {
  ////////////////////////////////////////////////
  // Immediate Type instructions
  ////////////////////////////////////////////////

  // ADDI
  private def ADDI(rs1: Int, imm: Int) {
    poke(c.io.rs1, rs1)
    poke(c.io.imm, imm)
    poke(c.io.op, 0) // b000
    poke(c.io.op_code, 19) // b0010011
    step(1)
    expect(c.io.result, rs1 + imm)
  }

  // Basic Functionality
  // Simple Test: two positive values 5 + 1 = 6
  ADDI(5, 1)
  // Positive rs1 and Negative immediate 5 + (-1) = 4
  ADDI(5, -1)
  // Negative rs1 and Positive immediate -5 + 1 = -4
  ADDI(-5, 1)
  // Negative rs1 and Negative immediate -5 + (-1) = -6
  ADDI(-5, -1)

  // XORI
  private def XORI(rs1: Int, imm: Int) {
    poke(c.io.rs1, rs1)
    poke(c.io.imm, imm)
    poke(c.io.op, 4) // b100
    poke(c.io.op_code, 19) // b0010011
    step(1)
    expect(c.io.result, rs1 ^ imm)
  }

  // ORI
  private def ORI(rs1: Int, imm: Int) {
    poke(c.io.rs1, rs1)
    poke(c.io.imm, imm)
    poke(c.io.op, 6) // b110
    poke(c.io.op_code, 19) // b0010011
    step(1)
    expect(c.io.result, rs1 | imm)
  }

  // ANDI
  private def ANDI(rs1: Int, imm: Int) {
    poke(c.io.rs1, rs1)
    poke(c.io.imm, imm)
    poke(c.io.op, 7) // b111
    poke(c.io.op_code, 19) // b0010011
    step(1)
    expect(c.io.result, rs1 & imm)
  }

  // SLLI
  private def SLLI(rs1: Int, imm: Int) {
    val special_imm = 31 & imm // h_0000_001f
    poke(c.io.rs1, rs1)
    poke(c.io.imm, special_imm)
    poke(c.io.op, 1) // b001
    poke(c.io.op_code, 19) // b0010011
    step(1)
    expect(c.io.result, rs1 << special_imm)
  }

  // SRAI
  private def SRAI(rs1: Int, imm: Int) {
    val special_imm_2_shift =  31 & imm // h_0000_001f
    val special_imm =  1024 | special_imm_2_shift // h_0000_0400
    poke(c.io.rs1, rs1)
    poke(c.io.imm, special_imm)
    poke(c.io.op, 5) // b101
    poke(c.io.op_code, 19) // b0010011
    step(1)
    expect(c.io.result, rs1 >> special_imm_2_shift)
  }

  // Fuzz here
  // Generate a random rs1 value and a random immediate
  for (i <- 0 until 10000) {
    val rs1 = rnd.nextInt(2000000000)
    val imm = rnd.nextInt(2000000000)
    ADDI(rs1, imm)
    XORI(rs1, imm)
    ORI(rs1, imm)
    ANDI(rs1, imm)
    SLLI(rs1, imm)
    SRAI(rs1, imm)
  }
}

/**
* This is a trivial example of how to run this Specification
* From within sbt use:
* {{{
* testOnly chutils.test.RegisterFileTester
* }}}
* From a terminal shell use:
* {{{
* sbt 'testOnly chutils.test.RegisterFileTester'
* }}}
*/
class ALUTester extends ChiselFlatSpec {
  // Generate ALU configuration
  val config = new AdeptConfig

  private val backendNames = if(firrtl.FileUtils.isCommandAvailable("verilator")) {
    Array("firrtl", "verilator")
  }
  else {
    Array("firrtl")
  }
  for ( backendName <- backendNames ) {
    "ALU" should s"store random data (with $backendName)" in {
      Driver(() => new ALU(config), backendName) {
        c => new ALUUnitTester(c)
      } should be (true)
    }
  }

  "Basic test using Driver.execute" should "be used as an alternative way to run specification" in {
    iotesters.Driver.execute(Array(), () => new ALU(config)) {
      c => new ALUUnitTester(c)
    } should be (true)
  }

  "using --backend-name verilator" should "be an alternative way to run using verilator" in {
    if(backendNames.contains("verilator")) {
      iotesters.Driver.execute(Array("--backend-name", "verilator"), () => new ALU(config)) {
        c => new ALUUnitTester(c)
      } should be(true)
    }
  }

  "running with --is-verbose" should "show more about what's going on in your tester" in {
    iotesters.Driver.execute(Array("--is-verbose"), () => new ALU(config)) {
      c => new ALUUnitTester(c)
    } should be(true)
  }

  "running with --fint-write-vcd" should "create a vcd file from your test" in {
    iotesters.Driver.execute(Array("--fint-write-vcd"), () => new ALU(config)) {
      c => new ALUUnitTester(c)
    } should be(true)
  }

  "using --help" should s"show the many options available" in {
    iotesters.Driver.execute(Array("--help"), () => new ALU(config)) {
      c => new ALUUnitTester(c)
    } should be (true)
  }
}
