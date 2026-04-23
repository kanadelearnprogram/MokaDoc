declare namespace API {
  type BaseResponseBoolean = {
    code?: number
    data?: boolean
    message?: string
  }

  type BaseResponseLoginVO = {
    code?: number
    data?: LoginVO
    message?: string
  }

  type BaseResponseLong = {
    code?: number
    data?: number
    message?: string
  }

  type BaseResponsePageQaMessage = {
    code?: number
    data?: PageQaMessage
    message?: string
  }

  type BaseResponsePageQaSession = {
    code?: number
    data?: PageQaSession
    message?: string
  }

  type BaseResponseUserVO = {
    code?: number
    data?: UserVO
    message?: string
  }

  type deleteSessionParams = {
    sessionId: number
  }

  type getUserByIdParams = {
    id: number
  }

  type listChatParams = {
    sessionId: number
    pageSize?: number
    lastCreateTime?: string
  }

  type listSessionsParams = {
    pageSize?: number
    lastCreateTime?: string
  }

  type LoginVO = {
    user?: UserVO
    token?: string
  }

  type PageQaMessage = {
    records?: QaMessage[]
    pageNumber?: number
    pageSize?: number
    totalPage?: number
    totalRow?: number
    optimizeCountQuery?: boolean
  }

  type PageQaSession = {
    records?: QaSession[]
    pageNumber?: number
    pageSize?: number
    totalPage?: number
    totalRow?: number
    optimizeCountQuery?: boolean
  }

  type QaMessage = {
    id?: number
    sessionId?: number
    messageType?: number
    content?: string
    createTime?: string
    deleteFlag?: number
  }

  type QaSession = {
    id?: number
    userId?: number
    sessionName?: string
    summary?: string
    createTime?: string
    updateTime?: string
    deleteFlag?: number
  }

  type SseEmitter = {
    timeout?: number
  }

  type UserLoginRequest = {
    account?: string
    password?: string
  }

  type UserQuestion = {
    content?: string
    sessionId?: number
  }

  type UserRegisterRequest = {
    username?: string
    email?: string
    password?: string
    nickname?: string
  }

  type UserVO = {
    id?: number
    username?: string
    email?: string
    nickname?: string
    avatar?: string
    status?: number
    registerTime?: string
    updateTime?: string
    password?: string
  }
}
