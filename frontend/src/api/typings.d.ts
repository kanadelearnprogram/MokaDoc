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

  type BaseResponseUserVO = {
    code?: number
    data?: UserVO
    message?: string
  }

  type chatParams = {
    prompt: string
  }

  type getUserByIdParams = {
    id: number
  }

  type LoginVO = {
    user?: UserVO
    token?: string
  }

  type UserLoginRequest = {
    account?: string
    password?: string
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
