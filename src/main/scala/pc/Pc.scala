package adept.pc

import chisel3._
import chisel3.util._

import adept.config.AdeptConfig

class BranchOpConstants { // join this group with all the rest of the configurations
  // Branch Type
  val BR_NE    = 1.asUInt(3.W)  // Branch on NotEqual
  val BR_EQ    = 0.asUInt(3.W)  // Branch on Equal
  val BR_GE    = 5.asUInt(3.W)  // Branch on Greater/Equal
  val BR_GEU   = 7.asUInt(3.W)  // Branch on Greater/Equal Unsigned
  val BR_LT    = 4.asUInt(3.W)  // Branch on Less Than
  val BR_LTU   = 6.asUInt(3.W)  // Branch on Less Than Unsigned
}

class Pc(config: AdeptConfig, br: BranchOpConstants) extends Module{
  val io = IO(new Bundle {
    // flags for branch confirmation
    val br_flags   = Input(Bool()) // branch verification flag
    // In from decoder
    val in_opcode  = Input(UInt((config.op_code + config.funct).W)) // opecode(7 bits) + function(3 bits) from word
    // Jump Adress in
    val br_step    = Input(SInt(config.XLen.W)) // In case of JALR
    // Offset
    val br_offset  = Input(SInt(config.XLen.W))
    // Program count after 1st pipeline level
    val pc_in      = Input(UInt(config.XLen.W))
    // Stall flag in case of brunch
    val stall      = Input(Bool())
    // Stall delayed by 1 clock
    val stall_re   = Input(Bool())
    // Program count to be sent for calc of new PC or for storage
    val pc_out     = Output(UInt(config.XLen.W))
  })

  // Conditional Branch flags attribution
  val Cond_Br_Ver = MuxLookup (io.in_opcode (9,7), false.B,
    Array(br.BR_EQ  -> ~io.br_flags,
          br.BR_NE  -> io.br_flags,
          br.BR_LT  -> io.br_flags,
          br.BR_GE  -> ~io.br_flags,
          br.BR_LTU -> io.br_flags,
          br.BR_GEU -> ~io.br_flags))

  // Offset selection criteria: is it a jump? and (is it JAL? or is it Conditional with correct flags?)
  val offset_sel      = io.stall &
                          ((io.in_opcode(3) & io.in_opcode(2)) |
                             ((~(io.in_opcode(3) | io.in_opcode(2))) & Cond_Br_Ver))

  // Auxiliar variable that contains either offset or 1
  val add_to_pc_val   = Mux(offset_sel,
                         Cat(Fill(2, io.br_offset(31)), io.br_offset(31,2)), 1.U)

  // next pc calculation
  val next_pc         = Mux(io.stall, io.pc_in, io.pc_out).asSInt + add_to_pc_val.asSInt
  val jalrORpc_select = Mux( (io.stall & (~io.in_opcode(3) & io.in_opcode(2))),
                               io.br_step, next_pc)
  val progCount       = RegInit(0.S(config.XLen.W))
  when (!io.stall_re){
    progCount := jalrORpc_select
  }
   io.pc_out  := progCount.asUInt
}

