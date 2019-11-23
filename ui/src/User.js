import React, {Component} from 'react';
import { withRouter } from 'react-router-dom';
import './Common.css';
import './User.css';
import MessagesLoader from "./MessagesLoader";

class User extends Component {
  constructor(props) {
    super(props);
    this.state = {
      userName: '',
      email: '',
      password: '',
      messages: MessagesLoader.Empty
    };
  }

  msg = (key) => {
    return this.state.messages(key);
  }

  componentDidMount = async() => {
    try {
      this.setState({
        messages: await new MessagesLoader().load([
          { key: 'userMaintenance'},
          { key: 'userName'},
          { key: 'email'},
          { key: 'password'},
          { key: 'register'},
          { key: 'registerCompleted'},
          { key: 'duplicated'},
          { key: 'error.unknown'}
        ])
      });
    } catch (e) {
      console.log("Error: " + JSON.stringify(e));
    }
  }

  onCreateUser = async(ev) => {
    try {
      this.setState({
        userNameError: undefined,
        passwordError: undefined,
        emailError: undefined,
        globalError: undefined,
        message: undefined
      });

      const resp = await fetch(
        '/api/createUser', {
          method: "POST",
          headers: {
            "Content-Type": "application/json",
            "Csrf-Token": "nocheck"
          },
          body: JSON.stringify({
            userName: this.state.userName,
            password: this.state.password,
            email: this.state.email
          })
        }
      );

      console.log("status: " + resp.status);
      if (resp.status === 200) {
        this.setState({
          userName: '',
          password: '',
          email: '',
          message: this.msg("registerCompleted")
        });
      } else if (resp.status === 400) {
        const json = await resp.json();
        console.log(JSON.stringify(json));
        this.setState({
          globalError: json[''],
          userNameError: json['userName'],
          passwordError: json['password'],
          emailError: json['email']
        });
      } else if (resp.status === 401) {
        this.props.history.push("/login/admin")
      } else if (resp.status === 409) {
        this.setState({
          userNameError: [this.msg('duplicated')]
        });
      } else {
        this.setState({
          globalError: this.msg('error.unknown')
        });
        console.log("error: " + resp.status);
      }
    } catch (e) {
      console.log("error: " + JSON.stringify(e));
    }
  }

  render() {
    const userNameError = this.state.userNameError !== undefined ?
      <div className="error">
        { this.state.userNameError.map((e, i) => <div key={i}>{e}</div>) }
      </div>
      : "";

    const passwordError = this.state.passwordError !== undefined ?
      <div className="error">
        { this.state.passwordError.map((e, i) => <div key={i}>{e}</div>) }
      </div>
      : "";

    const emailError = this.state.emailError !== undefined ?
      <div className="error">
        { this.state.emailError.map((e, i) => <div key={i}>{e}</div>) }
      </div>
      : "";

    return (
      <div className="user">
        { this.state.message !== undefined &&
          <article className="message is-info">
            <div className="message-body">{this.state.message}</div>
          </article>
        }

        { this.state.globalError !== undefined && 
          <article className="message is-danger">
            <div className="message-body">{this.state.globalError}</div>
          </article>
        }

        <nav className="panel">
          <p className="panel-heading">
            {this.msg('userMaintenance')}
          </p>
          <div className="panel-block">
            <form>
              <div className="field">
                <div className="control userName">
                  <label className="label">{this.msg('userName')}</label>
                  <div className="control">
                    <input id="userName" className="input" type="text" placeholder={this.msg('userName')}
                           value={this.state.userName} onChange={(e) => this.setState({userName: e.target.value})}
                    />
                  </div>
                  { userNameError }
                </div>
              </div>

              <div className="field">
                <div className="control email">
                  <label className="label">{this.msg('email')}</label>
                  <div className="control">
                    <input id="email" className="input" type="email" placeholder={this.msg('email')}
                           value={this.state.email} onChange={(e) => this.setState({email: e.target.value})}
                    />
                  </div>
                  { emailError }
                </div>
              </div>

              <div className="field">
                <div className="control password">
                  <label className="label">{this.msg('password')}</label>
                  <div className="control">
                    <input id="password" className="input" type="password" placeholder={this.msg('password')}
                           value={this.state.password} onChange={(e) => this.setState({password: e.target.value})}
                    />
                  </div>
                  { passwordError }
                </div>
              </div>

              <div className="field">
                <p className="control">
                  <button id="createUser" type="button" className="button is-success"
                          onClick={this.onCreateUser}>{this.msg("register")}</button>
                </p>
              </div>
            </form>
          </div>
        </nav>
      </div>
    );
  }
}

export default withRouter(User);
