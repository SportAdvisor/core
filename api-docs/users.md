### User registration
- **url** : `POST /api/users/sign-up`
- **body** : `{email: string, password: string, name: string}`
	<br/>*validations*: email by pattern `".+@.+\\..+"`, password by pattern `"^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)[a-zA-Z\\d]{8,}$"`, name not empty
- **response** 
	- success: 
	``
	{
  code:  200,
  data: {
    data: {
      token: string,
      refreshToken: string,
      expireAt: datetime
    },
    _links: null
  }
}`` 
	- error: 
	``{
  code:  400,
  errors: [
    {
      field: string,
      code: string
    }
  ]
}``
<br/>*possible mistakes* : `{"field": "email", "code": "0x1"}`, `{"field": "password", "code": "0x1"}`, `{"field": "email", "code": "0x2"}` where `0x1` - invalid field, `0x2` - duplication error *(if user already exists)*
