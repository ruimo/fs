import React, {Component} from 'react';
import { withRouter } from 'react-router-dom';
import './Admin.css';
import MessagesLoader from "./MessagesLoader";
import cx from 'classnames';

class Admin extends Component {
  /* Prevent error: perform a React state update on an unmounted
   * component. This is a no-op, but it indicates a memory leak in
   * your application. To fix, cancel all subscriptions and
   * asynchronous tasks in the componentWillUnmount method. */
  _isMounted = false;

  constructor(props) {
    super(props);
    this.state = {
      messages: MessagesLoader.Empty,
      loginUser: undefined
    };
  }

  async componentDidMount() {
    this._isMounted = true;
    try {
      const resp = await fetch("/api/loginInfo");
      if (resp.status === 200) {
        const user = await resp.json();
        console.log("loginInfo: " + JSON.stringify(user));
        this.setState({
          loginUser: user
        });
      } else if (resp.status === 404) {
        console.log("Login needed");
        this.setState({
          loginUser: undefined
        });
        this.props.history.push("/login/admin")
      }
    } catch (e) {
      console.log("Error: " + JSON.stringify(e));
    }

    try {
      if (this._isMounted) {
        this.setState({
          messages: await new MessagesLoader().load([
            { key: 'adminMenu'},
            { key: 'siteMaintenance'},
            { key: 'userMaintenance'}
          ])
        });
      }
    } catch (e) {
      console.log("Error: " + JSON.stringify(e));
    }
  }

  componentWillUnmount() {
    this._isMounted = false;
  }

  msg = (key) => {
    return this.state.messages(key);
  }

  startSiteMaintenance = () => {
    this.props.history.push("/site")
  }

  startUserMaintenance = () => {
    this.props.history.push("/user")
  }

  render() {
    const body = () => {
      return (
        <div>
          <nav className="panel">
            <p className="panel-heading">
              {this.msg('adminMenu')}
            </p>
            <p className="panel-block" >
              <a id="siteMaintenance" href="#siteMaintenance" className="is-fullwidth"
                 onClick={this.startSiteMaintenance}>
                {this.msg('siteMaintenance')}
              </a>
            </p>

            <p className={cx("panel-block", {'is-hidden': this.state.loginUser === undefined || this.state.loginUser.role !== 0})} >
              <a id="userMaintenance" href="#userMaintenance" className="is-fullwidth"
                 onClick={this.startUserMaintenance}>
                {this.msg('userMaintenance')}
              </a>
            </p>
          </nav>
        </div>
      );
    };

    return (
      <div className="admin">
        { body() }
      </div>
    );
  }
}

export default withRouter(Admin);
