package dev.devault.workspace.controller

import dev.devault.workspace.service.WorkspaceMemberService
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("")
class WorkspaceMemberController(
    private val workspaceMemberService: WorkspaceMemberService
) {


}